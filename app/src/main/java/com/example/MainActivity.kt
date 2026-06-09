package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: StoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Listeners for UI states from ViewModel
    val isSimMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()
    val userLocation by viewModel.currentUserLocation.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val maxDistanceKm by viewModel.maxDistanceKm.collectAsStateWithLifecycle()
    val minRating by viewModel.minRating.collectAsStateWithLifecycle()
    val sortByDistance by viewModel.sortByDistance.collectAsStateWithLifecycle()
    val storesList by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedStoreId by viewModel.selectedStoreId.collectAsStateWithLifecycle()
    val geminiUiState by viewModel.geminiUiState.collectAsStateWithLifecycle()

    var showFavoritesOnly by remember { mutableStateOf(false) }
    val favoriteStores by viewModel.favoriteStores.collectAsStateWithLifecycle()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.setSimulationMode(false)
            Toast.makeText(context, "Đã bật định vị GPS thực tế!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setSimulationMode(true)
            Toast.makeText(context, "Quyền định vị bị từ chối. Đã chuyển về vị trí Mô phỏng tại TP.HCM.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Subtle brand gradient header background
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00562D),
                                    Color(0xFF008848).copy(alpha = 0.95f)
                                )
                            )
                        )
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Brand label and location toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFCB05),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "BHX",
                                    tint = Color(0xFF00562D),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "BÁCH HÓA XANH",
                                color = Color(0xFFFFCB05),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Trợ Lý Tìm Kiếm & Đầu Bếp AI",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Simulated/Real Loc Switcher
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (isSimMode) {
                                    // Request permissions before disabling sim
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasFine || hasCoarse) {
                                        viewModel.setSimulationMode(false)
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                } else {
                                    viewModel.setSimulationMode(true)
                                }
                            }
                            .testTag("location_mode_toggle"),
                        color = if (isSimMode) Color(0xFFFFCB05).copy(alpha = 0.2f) else Color(0xFFE2F6EC).copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            if (isSimMode) Color(0xFFFFCB05) else Color(0xFFE2F6EC)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSimMode) Icons.Default.LocationOff else Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (isSimMode) Color(0xFFFFCB05) else Color(0xFFE2F6EC),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimMode) "Mô Phỏng" else "Định Vị Thực",
                                color = if (isSimMode) Color(0xFFFFCB05) else Color(0xFFE2F6EC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GPS Address Status line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSimMode) {
                            "Vị trí: Quận 1, Tp. Hồ Chí Minh (Định tâm 10.7769, 106.7009)"
                        } else {
                            "Vị trí GPS: %.5f, %.5f (Cách các cửa hàng thực tế)".format(userLocation.latitude, userLocation.longitude)
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filters section card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Search Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_search_input"),
                        placeholder = { Text(text = stringResource(R.string.search_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Xóa tìm kiếm",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Distance Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bán kính quét: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "%.1f km".format(maxDistanceKm),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Slider(
                        value = maxDistanceKm.toFloat(),
                        onValueChange = { viewModel.maxDistanceKm.value = it.toDouble() },
                        valueRange = 0.5f..10.0f,
                        steps = 19, // Every 0.5Km
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("distance_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating chips
                    Text(
                        text = "Bộ lọc đánh giá cao & Phân loại:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick filter rating keys
                        val ratingOptions = listOf(
                            0.0 to "Tất cả",
                            4.0 to "4.0+ ★",
                            4.5 to "4.5+ ★",
                            4.8 to "4.8+ ★"
                        )

                        ratingOptions.forEach { (stars, label) ->
                            val isSelected = minRating == stars
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.minRating.value = stars },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort order and favorites toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sorter action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.sortByDistance.value = !sortByDistance }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (sortByDistance) Icons.Default.Sort else Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sortByDistance) "Xếp theo: Gần nhất" else "Xếp theo: Đánh giá sao",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Favorite list toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFavoritesOnly = !showFavoritesOnly }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (showFavoritesOnly) Color.Red else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showFavoritesOnly) "Đã thích (${favoriteStores.size})" else "Tất cả",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showFavoritesOnly) Color.Red else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle state info
            val displayList = if (showFavoritesOnly) {
                storesList.filter { it.entity.isFavorite }
            } else {
                storesList
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showFavoritesOnly) "DANH SÁCH CỬA HÀNG YÊU THÍCH:" else "CỬA HÀNG TRONG KHU VỰC (5KM):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tìm thấy: ${displayList.size} cửa hàng",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Interactive Map Radar + Store items
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Không tìm thấy cửa hàng nào đáp ứng bộ lọc!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hãy tăng bán kính quét hoặc hạ bớt tiêu chuẩn đánh giá sao để quét diện rộng hơn.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("stores_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Let's feature our Beautiful Canvas Compass radar at the top of the list!
                    item {
                        Text(
                            text = "BẢN ĐỒ QUÉT KHÔNG GIAN (CHẠM VÀO ĐIỂM ĐẾN PHÍA DƯỚI):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SpatialRadarCompass(
                            userLocation = userLocation,
                            stores = displayList,
                            maxRadiusKm = maxDistanceKm,
                            onStoreSelected = { viewModel.selectedStoreId.value = it }
                        )
                    }

                    items(
                        items = displayList,
                        key = { it.entity.id }
                    ) { storeUi ->
                        val isSelected = selectedStoreId == storeUi.entity.id
                        StoreItemCard(
                            storeUi = storeUi,
                            isSelected = isSelected,
                            onToggleFavorite = { viewModel.toggleFavorite(storeUi.entity.id, storeUi.entity.isFavorite) },
                            onCardClick = {
                                viewModel.selectedStoreId.value = if (isSelected) null else storeUi.entity.id
                                viewModel.clearGeminiState()
                            },
                            onAskGemini = { viewModel.getAiMealSuggestions(storeUi.entity) },
                            geminiUiState = if (isSelected) geminiUiState else GeminiUiState.Idle,
                            onClearGemini = { viewModel.clearGeminiState() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas-Based Radar Compass drawing the physical offsets of Bach Hoa Xanh channels relative to user!
 * Avoids boring flat views, elevates visual fidelity.
 */
@Composable
fun SpatialRadarCompass(
    userLocation: UserLocation,
    stores: List<StoreUiItem>,
    maxRadiusKm: Double,
    onStoreSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = Color(0xFFFFCB05)

    // Pulse animation to make radar feel live
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val radarPulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulseRadius"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071F11),
                        Color(0xFF0F3A22)
                    )
                )
            )
            .testTag("spatial_radar")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stores, maxRadiusKm) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val maxCanvasRadius = (canvasWidth.coerceAtMost(canvasHeight) / 2f) * 0.85f

                        // Find the closest store representation clicked
                        var closestStoreId: Int? = null
                        var minClickDist = 48.0 // 48px target zone

                        for (store in stores) {
                            // Calculate store offset on canvas
                            val dxKm = getLongitudeDistanceKm(store.entity.longitude - userLocation.longitude, userLocation.latitude)
                            val dyKm = getLatitudeDistanceKm(store.entity.latitude - userLocation.latitude)

                            // Scale relative to max sweep radius
                            val scale = maxCanvasRadius / maxRadiusKm
                            val px = center.x + (dxKm * scale).toFloat()
                            val py = center.y - (dyKm * scale).toFloat() // Flip screen y coordinates

                            val clickDx = offset.x - px
                            val clickDy = offset.y - py
                            val clickDist = Math.sqrt((clickDx * clickDx + clickDy * clickDy).toDouble())

                            if (clickDist < minClickDist) {
                                minClickDist = clickDist
                                closestStoreId = store.entity.id
                            }
                        }

                        if (closestStoreId != null) {
                            onStoreSelected(closestStoreId)
                            Toast.makeText(context, "Đã chọn cửa hàng trên Radar!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val maxRadius = (width.coerceAtMost(height) / 2f) * 0.85f

            // 1. Draw radar sweep circles
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f * (1f - radarPulseRadius)),
                radius = maxRadius * radarPulseRadius,
                center = center
            )

            // Dynamic grid lines matching distance markers
            val stepsGrid = listOf(0.33f, 0.66f, 1.0f)
            stepsGrid.forEach { fraction ->
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = maxRadius * fraction,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                    )
                )
            }

            // Horizontal & vertical indicator crosshair lines
            drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Inner boundary text rings
            drawCircle(
                color = accentColor.copy(alpha = 0.6f),
                radius = 6.dp.toPx(),
                center = center
            )

            // Draw stores corresponding to physical location coordinates relative to center (0 km)
            for (store in stores) {
                val dxKm = getLongitudeDistanceKm(store.entity.longitude - userLocation.longitude, userLocation.latitude)
                val dyKm = getLatitudeDistanceKm(store.entity.latitude - userLocation.latitude)

                // Scale store position onto radar coordinates
                val scaleFactor = maxRadius / maxRadiusKm
                val pointX = center.x + (dxKm * scaleFactor).toFloat()
                val pointY = center.y - (dyKm * scaleFactor).toFloat() // Inverted screen canvas Y

                // Check constraints inside sweep radius
                if (store.distanceKm <= maxRadiusKm) {
                    // Draw store node
                    drawCircle(
                        color = if (store.entity.isFavorite) Color.Red else accentColor,
                        radius = 8.dp.toPx(),
                        center = Offset(pointX, pointY)
                    )
                    // Draw outer beacon core
                    drawCircle(
                        color = if (store.entity.isFavorite) Color.Red.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.4f),
                        radius = 16.dp.toPx(),
                        center = Offset(pointX, pointY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // Radar Compass HUD Text overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "PHẠM VI QUAY: %.1f KM".format(maxRadiusKm),
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Quét thấy ${stores.size} Bách Hóa Xanh",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Cửa hàng BHX",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Đã thích",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

// Distance estimation calculations
private fun getLatitudeDistanceKm(dLat: Double): Double {
    return dLat * 111.0 // 1 degree of latitude is roughly 111 km
}

private fun getLongitudeDistanceKm(dLon: Double, lat: Double): Double {
    return dLon * 111.0 * cos(Math.toRadians(lat))
}

@Composable
fun StoreItemCard(
    storeUi: StoreUiItem,
    isSelected: Boolean,
    onToggleFavorite: () -> Unit,
    onCardClick: () -> Unit,
    onAskGemini: () -> Unit,
    geminiUiState: GeminiUiState,
    onClearGemini: () -> Unit
) {
    val context = LocalContext.current
    val store = storeUi.entity

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("item_card_${store.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Title, Rating and Favorite Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "BHX",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = store.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFCB05),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = store.rating.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${store.ratingCount} đánh giá)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Distance Tag Pill (Styled according to close spacing standards)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = if (storeUi.distanceMeters < 1000) {
                                "${storeUi.distanceMeters} m"
                            } else {
                                "%.1f km".format(storeUi.distanceKm)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (store.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (store.isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = store.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Extra Info & Stock tags (Fruits/Vegetables & Meat stock highlights)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockIndicator(label = "Rau xanh", status = store.vegetableStatus)
                    StockIndicator(label = "Thịt tươi", status = store.meatStatus)
                }

                // Deals flash badge
                if (store.discountPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Red,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "Ưu đãi -${store.discountPercent}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Expanded AI suggestion details
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "THÔNG TIN LIÊN HỆ & HOẠT ĐỘNG:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Hotline: ${store.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Giờ mở cửa: ${store.hours}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Share / Direct Navigation Launcher
                        Row {
                            IconButton(
                                onClick = {
                                    val mapUri = Uri.parse("google.navigation:q=${store.latitude},${store.longitude}&mode=d")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        // Fallback to web browser maps
                                        val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${store.latitude},${store.longitude}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                    }
                                },
                                modifier = Modifier
                                    .shadow(1.dp, CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = "Chỉ đường",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chef AI segment
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = Color(0xFFFFCB05),
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = Color(0xFF00562D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gợi Ý Món Ngon Chef AI",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gemini sẽ dựa trên tình trạng rau xanh, thịt cá thực tế tại cửa hàng để lập công thức ẩm thực hôm nay cho bạn.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action button based on state
                            when (geminiUiState) {
                                is GeminiUiState.Idle -> {
                                    Button(
                                        onClick = onAskGemini,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chef_ai_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFCB05),
                                            contentColor = Color(0xFF00562D)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Đầu Bếp AI Gợi Ý Thực Đơn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is GeminiUiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Đang hỏi Trụ sở AI Bách Hóa Xanh...",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                is GeminiUiState.Success -> {
                                    Column {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                        ) {
                                            SelectionContainer {
                                                // Display markdown content simply
                                                Text(
                                                    text = geminiUiState.recipeMarkdown,
                                                    modifier = Modifier.padding(10.dp),
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF2C2C2C),
                                                    fontWeight = FontWeight.Normal,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Công thức cung cấp bởi Gemini 3.5 Flash",
                                                fontSize = 9.sp,
                                                color = Color.LightGray
                                            )
                                            TextButton(onClick = onClearGemini) {
                                                Text(text = "Đóng ý tưởng", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                is GeminiUiState.Error -> {
                                    Column {
                                        Text(
                                            text = geminiUiState.message,
                                            fontSize = 12.sp,
                                            color = Color.Red,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = onAskGemini,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(text = "Thử lại", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockIndicator(label: String, status: String) {
    val context = LocalContext.current
    val color = when (status) {
        "Còn dồi dào" -> Color(0xFF008848)
        "Còn ít" -> Color(0xFFFFCB05)
        else -> Color.Red
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: $status",
                fontSize = 10.sp,
                color = if (status == "Còn ít") Color(0xFF9E7E00) else color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
