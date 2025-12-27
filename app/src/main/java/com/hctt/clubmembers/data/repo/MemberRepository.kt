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
            val avatarKey = when {
                avatarInput == null -> existing?.avatarUrl
                !existing?.avatarUrl.isNullOrBlank() -> existing?.avatarUrl
                !resolvedUid.isNullOrBlank() -> "${resolvedUid}.jpg"
                else -> "${id}.jpg"
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
            paymentAmount?.let { recordPayment(id, it) }
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
                resolvedUid?.let { "${it}.jpg" } ?: "${localId}.jpg"
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
        val now = Instant.now().toEpochMilli()
        dao.softDelete(id, now)
        runCatching {
            membersTable.delete { filter { filter("id", FilterOperator.EQ, id)} }
        }.onFailure { Log.e(tag, "remote delete failed", it) }
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
        membersTable.select()
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
        membersTable.upsert(member.toUpsertDto())
    }

    private suspend fun recordPayment(memberId: Long, amount: Double) {
        val now = Instant.now().toString()
        val dto = PaymentDto(id = null, createdAt = now, amount = amount, memberId = memberId)
        runCatching { paymentsTable.insert(dto) }
            .onFailure { Log.e(tag, "payment insert failed", it) }
    }

    private suspend fun uploadAvatar(path: String, data: InputStream) =
        storage.from(avatarBucketName).upload(path, data.readBytes(), upsert = true)

    suspend fun getAvatarUrl(path: String): String? =
        runCatching { storage.from(avatarBucketName).createSignedUrl(path, 30.minutes) }.getOrNull()

    private fun generateLocalId(): Long {
        val time = System.currentTimeMillis()
        val rand = Random.nextInt(0, 999)
        return time * 1000 + rand
    }
}
