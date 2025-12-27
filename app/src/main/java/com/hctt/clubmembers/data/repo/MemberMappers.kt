package com.hctt.clubmembers.data.repo

import com.hctt.clubmembers.data.db.MemberEntity
import com.hctt.clubmembers.data.network.MemberDto
import com.hctt.clubmembers.data.network.MemberUpsertDto
import com.hctt.clubmembers.domain.model.Member
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val instantFormatter = DateTimeFormatter.ISO_INSTANT
private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun parseInstantFlexible(value: String): Instant {
    // Supabase may return timestamps without a timezone; assume UTC in that case.
    return runCatching { Instant.parse(value) }
        .getOrElse { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
}

fun MemberDto.toEntity(): MemberEntity = MemberEntity(
    id = id ?: 0,
    name = name ?: "",
    email = email,
    phone = phone,
    expiration = expiration?.let { LocalDate.parse(it, dateFormatter) },
    avatarUrl = avatarUrl,
    paymentAmount = null,
    updatedAt = parseInstantFlexible(updatedAt),
    uid = uid
)

fun MemberEntity.toDomain(): Member = Member(
    id = id.takeIf { it > 0 },
    name = name,
    email = email,
    phone = phone,
    expiration = expiration,
    avatarUrl = avatarUrl,
    paymentAmount = paymentAmount,
    updatedAt = updatedAt,
    uid = uid
)

fun Member.toDto(): MemberDto = MemberDto(
    id = id?.takeIf { it > 0 },
    name = name,
    email = email,
    phone = phone,
    expiration = expiration?.let { dateFormatter.format(it) },
    avatarUrl = avatarUrl,
    updatedAt = instantFormatter.format(updatedAt),
    uid = uid
)

fun Member.toUpsertDto(): MemberUpsertDto = MemberUpsertDto(
    id = id?.takeIf { it > 0 },
    name = name,
    email = email,
    phone = phone,
    expiration = expiration?.let { dateFormatter.format(it) },
    avatarUrl = avatarUrl,
    updatedAt = instantFormatter.format(updatedAt),
    uid = uid
)
