package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.local.dao.LikedProductDao
import com.coworker.jjikmuk.data.local.entity.LikedProductEntity
import com.coworker.jjikmuk.domain.model.LikedProduct
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.repository.LikedProductRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LikedProductRepositoryImpl @Inject constructor(
    private val likedProductDao: LikedProductDao,
) : LikedProductRepository {

    override fun observeLikedProducts(): Flow<List<LikedProduct>> {
        return likedProductDao.observeLikedProducts().map { products ->
            products.map { product -> product.toDomain() }
        }
    }

    override fun observeLikedProductBarcodes(): Flow<Set<String>> {
        return likedProductDao.observeLikedProductBarcodes().map { barcodes ->
            barcodes.toSet()
        }
    }

    override suspend fun toggleLikedProduct(product: ProductDetail) {
        if (likedProductDao.isLikedProduct(product.barcode)) {
            likedProductDao.deleteLikedProduct(product.barcode)
        } else {
            likedProductDao.upsertLikedProduct(product.toEntity())
        }
    }

    private fun ProductDetail.toEntity(): LikedProductEntity {
        return LikedProductEntity(
            barcode = barcode,
            productName = productName,
            brandName = brandName,
            imageUrl = imageUrl,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun LikedProductEntity.toDomain(): LikedProduct {
        return LikedProduct(
            barcode = barcode,
            productName = productName,
            brandName = brandName,
            imageUrl = imageUrl,
        )
    }
}
