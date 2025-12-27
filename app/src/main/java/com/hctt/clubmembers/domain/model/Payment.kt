package com.hctt.clubmembers.domain.model

import java.time.Instant

data class Payment(
    val id: Long?,
    val amount: Double,
    val createdAt: Instant,
    val memberId: Long
)
