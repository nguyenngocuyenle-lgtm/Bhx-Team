package com.example.data

import kotlinx.coroutines.flow.Flow

class StoreRepository(private val storeDao: StoreDao) {
    val allStores: Flow<List<StoreEntity>> = storeDao.getAllStores()

    suspend fun insert(store: StoreEntity) = storeDao.insertStore(store)
    suspend fun update(store: StoreEntity) = storeDao.updateStore(store)
    suspend fun setFavorite(id: Int, isFav: Boolean) = storeDao.setFavorite(id, isFav)
    fun getStoreById(id: Int): Flow<StoreEntity?> = storeDao.getStoreById(id)
}
