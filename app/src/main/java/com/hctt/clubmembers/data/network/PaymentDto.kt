package com.hctt.clubmembers.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentDto(
    val id: Long? = null,
    @SerialName("created_at") val createdAt: String,
    val amount: Double,
    @SerialName("member_id") val memberId: Long
)
