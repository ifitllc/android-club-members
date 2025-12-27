package com.hctt.clubmembers.data.repo

import com.hctt.clubmembers.data.network.PaymentDto
import com.hctt.clubmembers.domain.model.Payment
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private fun parseInstantFlexible(value: String): Instant {
    return runCatching { Instant.parse(value) }
        .getOrElse { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
}

fun PaymentDto.toPaymentDomain(): Payment = Payment(
    id = id,
    amount = amount,
    createdAt = parseInstantFlexible(createdAt),
    memberId = memberId
)
