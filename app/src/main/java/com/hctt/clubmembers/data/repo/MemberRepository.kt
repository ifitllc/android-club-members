package com.hctt.clubmembers.data.repo

import android.util.Log
import com.hctt.clubmembers.data.db.MemberDao
import com.hctt.clubmembers.data.db.MemberEntity
import com.hctt.clubmembers.data.network.MemberDto
import com.hctt.clubmembers.data.network.PaymentDto
import com.hctt.clubmembers.data.network.SupabaseClientProvider
import com.hctt.clubmembers.domain.model.Member
import com.hctt.clubmembers.domain.model.Payment
import com.hctt.clubmembers.data.repo.toPaymentDomain
import com.hctt.clubmembers.data.repo.toUpsertDto
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@Singleton
class MemberRepository @Inject constructor(
    private val dao: MemberDao,
    private val supabase: SupabaseClientProvider
) {
    private val tag = "MemberRepository"
    private val membersTable get() = supabase.client.from("members")
    private val paymentsTable get() = supabase.client.from("payments")
    private val storage get() = supabase.client.storage
    private val avatarBucketName = "avatars"

    fun observeActive(): Flow<List<Member>> = dao.observeActive().map { list ->
        val today = LocalDate.now()
        list.filter { it.expiration == null || !it.expiration.isBefore(today) }
            .map { it.toDomain() }
    }

    fun observeExpired(): Flow<List<Member>> = dao.observeActive().map { list ->
        val today = LocalDate.now()
        list.filter { it.expiration?.isBefore(today) == true }
            .map { it.toDomain() }
    }

    suspend fun getExpiredPaginated(offset: Int, limit: Int, sortBy: String = "expiration", ascending: Boolean = false): List<Member> {
        val today = LocalDate.now()
        val filtered = dao.getAll()
            .filter { it.isDeleted.not() }
            .filter { it.expiration?.isBefore(today) == true }
        
        val sorted = when (sortBy) {
            "name" -> filtered.sortedBy { it.name.lowercase() }
            "created" -> filtered.sortedBy { it.id }
            else -> filtered.sortedBy { it.expiration }
        }
        
        val ordered = if (ascending) sorted else sorted.reversed()
        
        return ordered
            .drop(offset)
            .take(limit)
            .map { it.toDomain() }
    }

    fun searchExpired(term: String): Flow<List<Member>> =
        dao.search("%$term%").map { list ->
            val today = LocalDate.now()
            list.filter { it.expiration?.isBefore(today) == true }
                .map { it.toDomain() }
        }

    suspend fun getMember(id: Long): Member? = dao.getById(id)?.toDomain()

    suspend fun addOrUpdate(
        name: String,
        email: String?,
        phone: String?,
        expiration: LocalDate?,
        paymentAmount: Double?,
        avatarInput: InputStream?,
        avatarFilename: String?,
        existingId: Long? = null,
        uid: String? = null
    ): Member {
        val now = Instant.now()
        val id = existingId
        val resolvedUid = uid ?: supabase.client.auth.currentSessionOrNull()?.user?.id
        if (id != null) {
            val existing = dao.getById(id)
            val expirationChanged = existing?.expiration != expiration
            val paymentChanged = existing?.paymentAmount != paymentAmount
            val avatarKey = when {
                avatarInput == null -> existing?.avatarUrl
                !existing?.avatarUrl.isNullOrBlank() -> existing?.avatarUrl?.let { ensureAvatarKey(it, resolvedUid) }
                else -> ensureAvatarKey(avatarFilename ?: "$id.jpg", resolvedUid)
            }
            val avatarPath = if (avatarInput != null && !avatarKey.isNullOrBlank()) {
                runCatching { uploadAvatar(avatarKey!!, avatarInput) }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()?.let { avatarKey }
            } else avatarKey
            val entity = MemberEntity(
                id = id,
                name = name,
                email = email,
                phone = phone,
                expiration = expiration,
                avatarUrl = avatarPath ?: existing?.avatarUrl,
                paymentAmount = paymentAmount,
                updatedAt = now,
                uid = existing?.uid ?: resolvedUid,
                isDeleted = false
            )
            dao.upsert(entity)
            pushMember(entity.toDomain())
            if (paymentAmount != null) {
                if (expirationChanged) {
                    recordPayment(id, paymentAmount)
                } else if (paymentChanged) {
                    updateLatestPayment(id, paymentAmount)
                }
            }
            return entity.toDomain()
        } else {
            // Create locally first to obtain an id for consistent avatar naming
            val baseEntity = MemberEntity(
                name = name,
                email = email,
                phone = phone,
                expiration = expiration,
                avatarUrl = null,
                paymentAmount = paymentAmount,
                updatedAt = now,
                uid = resolvedUid,
                isDeleted = false
            )

            val localId = dao.insertAndReturn(baseEntity)

            val avatarKey = if (avatarInput != null) {
                ensureAvatarKey(avatarFilename ?: "$localId.jpg", resolvedUid)
            } else null
            val avatarPath = if (avatarInput != null && !avatarKey.isNullOrBlank()) {
                runCatching { uploadAvatar(avatarKey!!, avatarInput) }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()?.let { avatarKey }
            } else null

            val entityWithAvatar = baseEntity.copy(id = localId, avatarUrl = avatarPath)
            dao.upsert(entityWithAvatar)
            pushMember(entityWithAvatar.toDomain())
            paymentAmount?.let { recordPayment(localId, it) }
            return entityWithAvatar.toDomain()
        }
    }

    suspend fun markDeleted(id: Long?) {
        if (id == null) return
        val now = Instant.now()
        dao.softDelete(id, now.toEpochMilli())
        dao.getById(id)?.let { local ->
            val deleted = local.copy(isDeleted = true, updatedAt = now)
            dao.upsert(deleted)
            runCatching { pushMember(deleted.toDomain()) }
                .onFailure { Log.e(tag, "remote soft delete failed", it) }
        }
    }

    suspend fun renew(id: Long?, newExpiry: LocalDate) {
        if (id == null) return
        val member = dao.getById(id) ?: return
        val updated = member.copy(expiration = newExpiry, updatedAt = Instant.now())
        dao.upsert(updated)
        pushMember(updated.toDomain())
    }

    suspend fun getPayments(memberId: Long): List<Payment> = runCatching {
        paymentsTable.select { filter { filter("member_id", FilterOperator.EQ, memberId) } }
            .decodeList<PaymentDto>()
            .map { it.toPaymentDomain() }
            .sortedByDescending { it.createdAt }
    }.getOrDefault(emptyList())

    suspend fun getLocallyModifiedCount(): Int {
        return try {
            val remote = fetchRemoteMembers()
            val remoteById = remote.associateBy { it.id }
            val local = dao.getAll()
            local.count { localMember ->
                val remoteMember = remoteById[localMember.id]
                remoteMember == null || localMember.updatedAt.isAfter(remoteMember.updatedAt)
            }
        } catch (t: Throwable) {
            Log.e(tag, "getLocallyModifiedCount failed", t)
            0
        }
    }

    suspend fun pullLatest() {
        runCatching { mergeRemoteIntoLocal(fetchRemoteMembers()) }
            .onFailure { Log.e(tag, "pullLatest failed", it) }
    }

    suspend fun syncBidirectional() {
        try {
            val remote = fetchRemoteMembers()
            val remoteById = remote.associateBy { it.id }
            val local = dao.getAll()
            val localById = local.associateBy { it.id }

            // Push local changes that are newer than remote or missing remotely.
            local.forEach { localMember ->
                val remoteMember = remoteById[localMember.id]
                val shouldPush = remoteMember == null || localMember.updatedAt.isAfter(remoteMember.updatedAt)
                if (shouldPush) {
                    pushMember(localMember.toDomain())
                }
            }

            // Merge remote changes back into local (preserving local paymentAmount field).
            mergeRemoteIntoLocal(remote, localById)
        } catch (t: Throwable) {
            Log.e(tag, "syncBidirectional failed", t)
            throw t
        }
    }

    private suspend fun fetchRemoteMembers(): List<MemberEntity> =
        membersTable.select {
            filter {
                filter("is_deleted", FilterOperator.EQ, false)
            }
        }
            .decodeList<MemberDto>()
            .map { it.toEntity() }

    private suspend fun mergeRemoteIntoLocal(
        remote: List<MemberEntity>,
        localById: Map<Long, MemberEntity> = emptyMap()
    ) {
        Log.d(tag, "mergeRemoteIntoLocal remote size=${remote.size}")
        val remoteIds = remote.mapNotNull { it.id }
        if (remoteIds.isNotEmpty()) {
            dao.deleteNotIn(remoteIds)
        } else {
            dao.deleteAll()
        }
        remote.forEach { incoming ->
            val local = localById[incoming.id] ?: dao.getById(incoming.id)
            val shouldReplace = local == null || incoming.updatedAt.isAfter(local.updatedAt)
            if (shouldReplace) {
                val merged = if (local != null) incoming.copy(paymentAmount = local.paymentAmount) else incoming
                dao.upsert(merged)
            }
        }
    }

    private suspend fun pushMember(member: Member) {
        // Ensure we always include the current uid to satisfy RLS policies on Supabase.
        val uid = member.uid ?: supabase.client.auth.currentSessionOrNull()?.user?.id
            ?: error("No auth user available for push")
        val withUid = member.copy(uid = uid)

        // Explicitly upsert on id so updates (e.g., expiration changes) are applied remotely.
        membersTable.upsert(withUid.toUpsertDto(), onConflict = "id")
    }

    private suspend fun recordPayment(memberId: Long, amount: Double) {
        val now = Instant.now().toString()
        val dto = PaymentDto(id = null, createdAt = now, amount = amount, memberId = memberId)
        runCatching { paymentsTable.insert(dto) }
            .onFailure { Log.e(tag, "payment insert failed", it) }
    }

    private suspend fun updateLatestPayment(memberId: Long, amount: Double) {
        val latest = getPayments(memberId).firstOrNull() ?: return
        val latestId = latest.id ?: return
        val dto = PaymentDto(
            id = latestId,
            createdAt = latest.createdAt.toString(),
            amount = amount,
            memberId = memberId
        )
        runCatching { paymentsTable.upsert(dto, onConflict = "id") }
            .onFailure { Log.e(tag, "payment update failed", it) }
    }

    private suspend fun uploadAvatar(path: String, data: InputStream) =
        storage.from(avatarBucketName).upload(path, data.readBytes(), upsert = true)

    private fun ensureAvatarKey(key: String, uid: String?): String {
        if (uid.isNullOrBlank()) return key
        val normalized = key.trimStart('/')
        return if (normalized.startsWith("$uid/")) normalized else "$uid/$normalized"
    }

    suspend fun getAvatarUrl(path: String): String? =
        runCatching { storage.from(avatarBucketName).createSignedUrl(path, 30.minutes) }.getOrNull()

    private fun generateLocalId(): Long {
        val time = System.currentTimeMillis()
        val rand = Random.nextInt(0, 999)
        return time * 1000 + rand
    }
}
