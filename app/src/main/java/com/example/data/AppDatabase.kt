package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [StoreEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bach_hoa_xanh_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.storeDao())
                }
            }
        }

        suspend fun populateDatabase(storeDao: StoreDao) {
            val initialStores = listOf(
                StoreEntity(
                    name = "Bách Hóa Xanh Đinh Tiên Hoàng",
                    address = "18 Đinh Tiên Hoàng, Phường Đa Kao, Quận 1, TP. HCM",
                    latitude = 10.7876,
                    longitude = 106.6989,
                    rating = 4.7,
                    ratingCount = 452,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 20
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Phùng Khắc Khoan",
                    address = "12 Phùng Khắc Khoan, Phường Đa Kao, Quận 1, TP. HCM",
                    latitude = 10.7818,
                    longitude = 106.6953,
                    rating = 4.5,
                    ratingCount = 310,
                    phone = "1900 1908",
                    hours = "06:00 - 21:30",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn ít",
                    discountPercent = 10
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Nguyễn Đình Chiểu",
                    address = "145 Nguyễn Đình Chiểu, Phường Võ Thị Sáu, Quận 3, TP. HCM",
                    latitude = 10.7785,
                    longitude = 106.6908,
                    rating = 4.8,
                    ratingCount = 620,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 15
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Nguyễn Thị Minh Khai",
                    address = "56 Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. HCM",
                    latitude = 10.7842,
                    longitude = 106.6976,
                    rating = 4.9,
                    ratingCount = 182,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 25
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Điện Biên Phủ Q3",
                    address = "220 Điện Biên Phủ, Phường Võ Thị Sáu, Quận 3, TP. HCM",
                    latitude = 10.7885,
                    longitude = 106.6872,
                    rating = 4.6,
                    ratingCount = 230,
                    phone = "1900 1908",
                    hours = "06:00 - 21:30",
                    vegetableStatus = "Còn ít",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 0
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Trần Quang Khải",
                    address = "32 Trần Quang Khải, Phường Tân Định, Quận 1, TP. HCM",
                    latitude = 10.7915,
                    longitude = 106.6912,
                    rating = 4.4,
                    ratingCount = 510,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Tạm hết",
                    discountPercent = 5
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Nguyễn Công Trứ",
                    address = "104 Nguyễn Công Trứ, Phường Nguyễn Thái Bình, Quận 1, TP. HCM",
                    latitude = 10.7711,
                    longitude = 106.7001,
                    rating = 4.3,
                    ratingCount = 195,
                    phone = "1900 1908",
                    hours = "06:00 - 21:30",
                    vegetableStatus = "Tạm hết",
                    meatStatus = "Còn ít",
                    discountPercent = 10
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Khánh Hội",
                    address = "248 Khánh Hội, Phường 5, Quận 4, TP. HCM",
                    latitude = 10.7582,
                    longitude = 106.7021,
                    rating = 4.2,
                    ratingCount = 420,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 30
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Xô Viết Nghệ Tĩnh",
                    address = "80 Xô Viết Nghệ Tĩnh, Phường 21, Quận Bình Thạnh, TP. HCM",
                    latitude = 10.7932,
                    longitude = 106.7118,
                    rating = 4.6,
                    ratingCount = 540,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 0
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Điện Biên Phủ BT",
                    address = "195 Điện Biên Phủ, Phường 15, Quận Bình Thạnh, TP. HCM",
                    latitude = 10.7985,
                    longitude = 106.7112,
                    rating = 4.5,
                    ratingCount = 380,
                    phone = "1900 1908",
                    hours = "06:00 - 21:30",
                    vegetableStatus = "Còn ít",
                    meatStatus = "Còn ít",
                    discountPercent = 15
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Võ Duy Ninh",
                    address = "45 Võ Duy Ninh, Phường 22, Quận Bình Thạnh, TP. HCM",
                    latitude = 10.7928,
                    longitude = 106.7198,
                    rating = 4.1,
                    ratingCount = 150,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Tạm hết",
                    discountPercent = 5
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Phan Xích Long",
                    address = "38 Phan Xích Long, Phường 2, Quận Phú Nhuận, TP. HCM",
                    latitude = 10.7972,
                    longitude = 106.6885,
                    rating = 4.7,
                    ratingCount = 612,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 20
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Nguyễn Văn Đậu",
                    address = "63 Nguyễn Văn Đậu, Phường 6, Quận Bình Thạnh, TP. HCM",
                    latitude = 10.8062,
                    longitude = 106.6896,
                    rating = 4.8,
                    ratingCount = 480,
                    phone = "1900 1908",
                    hours = "06:00 - 21:30",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 0
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Lê Văn Sỹ",
                    address = "15 Lê Văn Sỹ, Phường 13, Quận Phú Nhuận, TP. HCM",
                    latitude = 10.7925,
                    longitude = 106.6785,
                    rating = 4.4,
                    ratingCount = 210,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 10
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Bến Thành",
                    address = "114 Nguyễn Trãi, Phường Bến Thành, Quận 1, TP. HCM",
                    latitude = 10.7709,
                    longitude = 106.6908,
                    rating = 4.7,
                    ratingCount = 340,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 15
                ),
                StoreEntity(
                    name = "Bách Hóa Xanh Nguyễn Hữu Cảnh",
                    address = "9 Nguyễn Hữu Cảnh, Phường 19, Quận Bình Thạnh, TP. HCM",
                    latitude = 10.7895,
                    longitude = 106.7095,
                    rating = 4.9,
                    ratingCount = 720,
                    phone = "1900 1908",
                    hours = "06:00 - 22:00",
                    vegetableStatus = "Còn dồi dào",
                    meatStatus = "Còn dồi dào",
                    discountPercent = 30
                )
            )
            storeDao.insertStores(initialStores)
        }
    }
}
