package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "bach_hoa_xanh_stores")
data class StoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val ratingCount: Int,
    val phone: String,
    val hours: String,
    val vegetableStatus: String, // "Còn dồi dào" (Abundant), "Còn ít" (Few), "Tạm hết" (Out)
    val meatStatus: String, // "Còn dồi dào", "Còn ít", "Tạm hết"
    val discountPercent: Int = 0, // 0 - 50% discount deals!
    val isFavorite: Boolean = false
) : Serializable
