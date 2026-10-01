package com.coworker.jjikmuk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_products")
data class LikedProductEntity(
    @PrimaryKey val barcode: String,
    val productName: String,
    val brandName: String,
    val imageUrl: String?,
    val createdAt: Long,
)
