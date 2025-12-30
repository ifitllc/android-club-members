package com.hctt.clubmembers.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberDto(
    val id: Long? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("expiration") val expiration: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    val uid: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)
