package com.hctt.clubmembers.domain.model

import java.time.Instant
import java.time.LocalDate

data class Member(
    val id: Long?,
    val name: String,
    val email: String?,
    val phone: String?,
    val expiration: LocalDate?,
    val avatarUrl: String?,
    val paymentAmount: Double?,
    val updatedAt: Instant,
    val uid: String? = null,
    val isDeleted: Boolean = false
)
