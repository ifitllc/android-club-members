package com.hctt.clubmembers.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
