package com.example.data

import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserLocation(val latitude: Double, val longitude: Double)

sealed class GeminiUiState {
    object Idle : GeminiUiState()
    object Loading : GeminiUiState()
    data class Success(val recipeMarkdown: String) : GeminiUiState()
    data class Error(val message: String) : GeminiUiState()
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = StoreRepository(db.storeDao())

    // Simulating district 1, HCMC as default fallback / simulation mode
    private val simulatedLocation = UserLocation(10.7769, 106.7009)

    // Location State
    val isSimulationMode = MutableStateFlow(true)
    val currentUserLocation = MutableStateFlow(simulatedLocation)

    // Filters and Query State
    val searchQuery = MutableStateFlow("")
    val maxDistanceKm = MutableStateFlow(5.0) // Defaults to 5km as requested
    val minRating = MutableStateFlow(0.0) // 0.0 means "Tất cả"
    val sortByDistance = MutableStateFlow(true) // true: Distance, false: Rating

    // Selected store for details or AI smart receipt suggestions
    val selectedStoreId = MutableStateFlow<Int?>(null)
    val geminiUiState = MutableStateFlow<GeminiUiState>(GeminiUiState.Idle)

    // Base database stream
    private val allStores = repository.allStores

    // UI state that computes filtered + sorted store entries, reactive to query, radius, rating, locations
    val uiState: StateFlow<List<StoreUiItem>> = combine(
        allStores,
        currentUserLocation,
        searchQuery,
        maxDistanceKm,
        minRating,
        sortByDistance
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val stores = flows[0] as List<StoreEntity>
        val userLoc = flows[1] as UserLocation
        val query = flows[2] as String
        val maxDist = flows[3] as Double
        val rating = flows[4] as Double
        val sortByDist = flows[5] as Boolean

        stores.map { storeEntity ->
            val dist = calculateDistanceInKm(
                userLoc.latitude, userLoc.longitude,
                storeEntity.latitude, storeEntity.longitude
            )
            val distInMeters = (dist * 1000).toInt()
            StoreUiItem(
                entity = storeEntity,
                distanceKm = dist,
                distanceMeters = distInMeters
            )
        }
        .filter { item ->
            // Search criteria: name or address matches
            val matchesQuery = query.isEmpty() || 
                item.entity.name.contains(query, ignoreCase = true) || 
                item.entity.address.contains(query, ignoreCase = true)
            
            // Distance criteria: within max distance
            val matchesDistance = item.distanceKm <= maxDist
            
            // Rating criteria
            val matchesRating = item.entity.rating >= rating

            matchesQuery && matchesDistance && matchesRating
        }
        .sortedWith { a, b ->
            if (sortByDist) {
                a.distanceKm.compareTo(b.distanceKm)
            } else {
                b.entity.rating.compareTo(a.entity.rating) // High rating first
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Keep track of favorited list
    val favoriteStores: StateFlow<List<StoreEntity>> = allStores.combine(MutableStateFlow(true)) { stores, _ ->
        stores.filter { it.isFavorite }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleFavorite(storeId: Int, currentFav: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(storeId, !currentFav)
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        isSimulationMode.value = enabled
        if (enabled) {
            currentUserLocation.value = simulatedLocation
        } else {
            requestGpsLocation()
        }
    }

    // Trigger standard Android GPS coordinates update safely
    private fun requestGpsLocation() {
        val context = getApplication<Application>().applicationContext
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null) {
            try {
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (!isSimulationMode.value) {
                            currentUserLocation.value = UserLocation(location.latitude, location.longitude)
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                if (isNetworkEnabled) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null)
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                        if (!isSimulationMode.value) {
                            currentUserLocation.value = UserLocation(it.latitude, it.longitude)
                        }
                    }
                }
                if (isGpsEnabled) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null)
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                        if (!isSimulationMode.value) {
                            currentUserLocation.value = UserLocation(it.latitude, it.longitude)
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Handle permission not granted yet - fallback to simulated
                isSimulationMode.value = true
                currentUserLocation.value = simulatedLocation
            }
        }
    }

    // Call Gemini API to recommend recipes using this store's status
    fun getAiMealSuggestions(store: StoreEntity) {
        geminiUiState.value = GeminiUiState.Loading
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    geminiUiState.value = GeminiUiState.Error(
                        "Vui lòng cấu hình API Key của bạn trong bảng điều hành Secrets của AI Studio để mở khóa tính năng Trợ Lý AI gợi ý chuẩn đầu bếp."
                    )
                    return@launch
                }

                val prompt = """
                    Bạn là trợ lý đầu bếp ảo chính thức của thương hiệu Bách Hóa Xanh (BHX). 
                    Hãy gợi ý 3 món ăn ngon của Việt Nam có thể chế biến được từ nguyên liệu tươi đang sẵn có tại cửa hàng '${store.name}' của chúng tôi. 
                    Thông tin cửa hàng hiện tại:
                    - Địa chỉ: ${store.address}
                    - Đánh giá khách hàng: ${store.rating} sao
                    - Trạng thái rau tươi: ${store.vegetableStatus}
                    - Trạng thái thịt mộc: ${store.meatStatus}
                    ${if (store.discountPercent > 0) "- Chương trình ưu đãi: Giảm giá ${store.discountPercent}% cho các mặt hàng tươi sống!" else ""}

                    Định dạng phản hồi:
                    1. Chào mừng khách hàng cực ngọt, nhấn mạnh lợi thế khoảng cách hoặc sự tươi ngon của BHX.
                    2. Kể tên 3 món ăn với icon sinh động. Với mỗi món ăn, hãy viết:
                       - 💡 Tại sao món này phù hợp với nguyên liệu đang có hôm nay ở cửa hàng.
                       - 🥕 Nguyên liệu cần mua ở BHX.
                       - 🍳 3 Bước chế biến nhanh chóng.
                    3. Gửi kèm lời chúc chúc ngon miệng.
                    
                    Ngôn ngữ: Tiếng Việt, trình bày tinh giản bằng Markdown, định dạng Bullet points rõ ràng, sử dụng phong phú Emojis rau củ thịt cá nấu nướng đặc trưng của ẩm thực Việt Nam.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    geminiUiState.value = GeminiUiState.Success(text)
                } else {
                    geminiUiState.value = GeminiUiState.Error("Không thể nhận phản hồi từ Gemini. Trợ lý đang bận chuẩn bị rau sống.")
                }
            } catch (e: Exception) {
                geminiUiState.value = GeminiUiState.Error("Đã xảy ra lỗi kết nối: ${e.localizedMessage}. Vui lòng kiểm tra lại kết nối mạng của bạn.")
            }
        }
    }

    fun clearGeminiState() {
        geminiUiState.value = GeminiUiState.Idle
    }

    // Haversine formula calculation
    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

data class StoreUiItem(
    val entity: StoreEntity,
    val distanceKm: Double,
    val distanceMeters: Int
)
