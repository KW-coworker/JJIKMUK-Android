package com.coworker.jjikmuk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coworker.jjikmuk.data.local.entity.LikedProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedProductDao {

    @Query(
        """
        SELECT *
        FROM liked_products
        ORDER BY createdAt DESC
        """,
    )
    fun observeLikedProducts(): Flow<List<LikedProductEntity>>

    @Query("SELECT barcode FROM liked_products")
    fun observeLikedProductBarcodes(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_products WHERE barcode = :barcode)")
    suspend fun isLikedProduct(barcode: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLikedProduct(product: LikedProductEntity)

    @Query("DELETE FROM liked_products WHERE barcode = :barcode")
    suspend fun deleteLikedProduct(barcode: String)
}
