package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ai.ReceiptImageProcessor
import com.example.data.ai.ReceiptTransactionKind
import com.example.data.ai.ScannedReceiptDraft
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.ReceiptScanUiState
import com.example.ui.components.BentoModalSheetDialog
import com.example.ui.components.CategorySearchPickerDialog
import com.example.ui.components.CategorySelectField
import com.example.ui.components.ReceiptScanSkeleton
import com.example.ui.components.SkeletonBox
import com.example.ui.components.WalletLogoBadge
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.pocketCategoryFor
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ReceiptScannerSheet(
    scanState: ReceiptScanUiState,
    wallets: List<WalletEntity>,
    pockets: List<PocketEntity>,
    currencyConfig: CurrencyDisplayConfig,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onResetToCapture: () -> Unit,
    onSwitchToManualPreview: () -> Unit,
    onDismiss: () -> Unit,
    onExternalFlowActive: (Boolean) -> Unit,
    onSaveTransaction: (
        existingId: String?,
        title: String,
        note: String,
        amount: Long,
        type: TransactionType,
        pocketId: String?,
        walletId: String?,
        categoryKey: String,
        categoryLabel: String,
        locationName: String,
        occurredAtMillis: Long,
    ) -> Unit,
    onSaveTransfer: (
        fromWalletId: String,
        toWalletId: String,
        amount: Long,
        note: String,
    ) -> Unit,
    onShowError: (String) -> Unit,
    onOpenManualTransaction: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        onExternalFlowActive(false)
        if (uri != null) {
            scope.launch {
                val bitmap = ReceiptImageProcessor.loadPreparedReceiptBitmap(context, uri)
                if (bitmap != null) {
                    onAnalyzeBitmap(bitmap)
                } else {
                    onShowError("Gagal membuka gambar struk dari galeri.")
                }
            }
        }
    }

    val launchGalleryPicker = {
        onExternalFlowActive(true)
        galleryPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    when (scanState) {
        ReceiptScanUiState.Idle -> {
            ReceiptCaptureDialog(
                onBitmapCaptured = onAnalyzeBitmap,
                onPickFromGallery = launchGalleryPicker,
                onOpenManualTransaction = onOpenManualTransaction,
                onExternalFlowActive = onExternalFlowActive,
                onShowError = onShowError,
                onDismiss = onDismiss,
            )
        }

        is ReceiptScanUiState.Analyzing -> {
            ReceiptAnalyzingDialog(
                previewBitmap = scanState.previewBitmap,
                onCancel = {
                    onResetToCapture()
                },
            )
        }

        is ReceiptScanUiState.Error -> {
            ReceiptErrorDialog(
                previewBitmap = scanState.previewBitmap,
                errorMessage = scanState.message,
                onRetryAi = {
                    scanState.previewBitmap?.let(onAnalyzeBitmap) ?: onResetToCapture()
                },
                onManualFill = onSwitchToManualPreview,
                onRetake = onResetToCapture,
                onDismiss = onDismiss,
            )
        }

        is ReceiptScanUiState.Preview -> {
            ReceiptPreviewEditorSheet(
                previewBitmap = scanState.previewBitmap,
                draft = scanState.draft,
                wallets = wallets,
                pockets = pockets,
                currencyConfig = currencyConfig,
                onRetake = onResetToCapture,
                onPickFromGallery = launchGalleryPicker,
                onDismiss = onDismiss,
                onSaveTransaction = { title, note, amountIdr, txType, pocketId, walletId, catKey, catLabel, locName, occurredAt ->
                    onSaveTransaction(
                        null,
                        title,
                        note,
                        amountIdr,
                        txType,
                        pocketId,
                        walletId,
                        catKey,
                        catLabel,
                        locName,
                        occurredAt,
                    )
                    onDismiss()
                },
                onSaveTransfer = { fromId, toId, amountIdr, note ->
                    onSaveTransfer(fromId, toId, amountIdr, note)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ReceiptCaptureDialog(
    onBitmapCaptured: (Bitmap) -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenManualTransaction: () -> Unit,
    onExternalFlowActive: (Boolean) -> Unit,
    onShowError: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraBindError by remember { mutableStateOf<String?>(null) }
    var flashEnabled by rememberSaveable { mutableStateOf(false) }
    var zoomRatio by rememberSaveable { mutableFloatStateOf(1f) }
    var isCapturing by remember { mutableStateOf(false) }

    val scanBeamTransition = rememberInfiniteTransition(label = "receipt_scan_beam")
    val scanBeamFraction by scanBeamTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_beam_y",
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onExternalFlowActive(false)
        hasCameraPermission = granted
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(hasCameraPermission, lifecycleOwner) {
        if (!hasCameraPermission) {
            onDispose {}
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            var providerRef: ProcessCameraProvider? = null
            val listener = Runnable {
                try {
                    val provider = cameraProviderFuture.get()
                    providerRef = provider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    provider.unbindAll()
                    val cam = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                    )
                    imageCapture = capture
                    boundCamera = cam
                    cameraBindError = null
                } catch (e: Exception) {
                    cameraBindError = e.localizedMessage ?: "Kamera belakang tidak tersedia di perangkat ini."
                }
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
            onDispose {
                runCatching { providerRef?.unbindAll() }
            }
        }
    }

    // Update flash & zoom live when toggled
    DisposableEffect(flashEnabled, zoomRatio, imageCapture, boundCamera) {
        imageCapture?.flashMode = if (flashEnabled) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        runCatching { boundCamera?.cameraControl?.setZoomRatio(zoomRatio) }
        onDispose {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07090C))
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (hasCameraPermission && cameraBindError == null) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                )

                // Tall + Comfortably Wide Receipt Framing Reticle Overlay (Never squat KTP card!)
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val topReserved = 70.dp
                    val bottomReserved = 164.dp
                    val availableHeight = (maxHeight - topReserved - bottomReserved).coerceAtLeast(300.dp)
                    val frameWidth = (maxWidth * 0.84f).coerceIn(250.dp, 420.dp)
                    val frameHeight = availableHeight
                        .coerceAtMost(frameWidth * 1.68f)
                        .coerceAtLeast(minOf(availableHeight, frameWidth * 1.28f))

                    val frameWidthPx = with(density) { frameWidth.toPx() }
                    val frameHeightPx = with(density) { frameHeight.toPx() }
                    val cornerRadiusPx = with(density) { 24.dp.toPx() }
                    val topReservedPx = with(density) { topReserved.toPx() }
                    val availableHeightPx = with(density) { availableHeight.toPx() }
                    val bracketLenPx = with(density) { 30.dp.toPx() }
                    val bracketStrokePx = with(density) { 4.dp.toPx() }

                    val accentColor = bento.accentTile

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val left = (size.width - frameWidthPx) / 2f
                        val top = topReservedPx + ((availableHeightPx - frameHeightPx) / 2f).coerceAtLeast(0f)
                        val right = left + frameWidthPx
                        val bottom = top + frameHeightPx

                        val fullPath = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                        }
                        val cutoutPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(left, top, right, bottom),
                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                )
                            )
                        }
                        val scrimPath = Path().apply {
                            op(fullPath, cutoutPath, PathOperation.Difference)
                        }
                        drawPath(
                            path = scrimPath,
                            color = Color.Black.copy(alpha = 0.56f),
                        )

                        // Subtle inner frame guide
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.24f),
                            topLeft = Offset(left, top),
                            size = Size(frameWidthPx, frameHeightPx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                            style = Stroke(width = 1.2.dp.toPx()),
                        )

                        // Animated scanning laser line across tall receipt window
                        val beamY = top + (frameHeightPx * scanBeamFraction)
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.9f),
                                    Color.White.copy(alpha = 0.95f),
                                    accentColor.copy(alpha = 0.9f),
                                    Color.Transparent,
                                ),
                                startX = left + 16f,
                                endX = right - 16f,
                            ),
                            start = Offset(left + 16f, beamY),
                            end = Offset(right - 16f, beamY),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                        )

                        // 4 crisp Bento corner brackets
                        val strokeStyle = Stroke(
                            width = bracketStrokePx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        )
                        val r = cornerRadiusPx * 0.85f
                        val arm = bracketLenPx

                        // Top-Left
                        drawPath(
                            Path().apply {
                                moveTo(left, top + r + arm)
                                lineTo(left, top + r)
                                quadraticTo(left, top, left + r, top)
                                lineTo(left + r + arm, top)
                            },
                            color = accentColor,
                            style = strokeStyle,
                        )
                        // Top-Right
                        drawPath(
                            Path().apply {
                                moveTo(right - r - arm, top)
                                lineTo(right - r, top)
                                quadraticTo(right, top, right, top + r)
                                lineTo(right, top + r + arm)
                            },
                            color = accentColor,
                            style = strokeStyle,
                        )
                        // Bottom-Left
                        drawPath(
                            Path().apply {
                                moveTo(left, bottom - r - arm)
                                lineTo(left, bottom - r)
                                quadraticTo(left, bottom, left + r, bottom)
                                lineTo(left + r + arm, bottom)
                            },
                            color = accentColor,
                            style = strokeStyle,
                        )
                        // Bottom-Right
                        drawPath(
                            Path().apply {
                                moveTo(right - r - arm, bottom)
                                lineTo(right - r, bottom)
                                quadraticTo(right, bottom, right, bottom - r)
                                lineTo(right, bottom - r - arm)
                            },
                            color = accentColor,
                            style = strokeStyle,
                        )
                    }

                    // Guidance pill neatly inside the top of the receipt frame
                    val topOffsetDp = topReserved + ((availableHeight - frameHeight) / 2f).coerceAtLeast(0.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffsetDp + 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.68f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Posisikan struk di dalam bingkai vertikal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }
            } else {
                // Permission required or unavailable camera state — offers immediate Gallery & Manual fallback
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(bento.accentTile.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = null,
                            tint = bento.accentTile,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Text(
                        text = if (cameraBindError != null) "Kamera Tidak Tersedia" else "Izin Kamera Dibutuhkan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Text(
                        text = cameraBindError
                            ?: "Izinkan kamera untuk memotret struk fisik, atau langsung upload tangkapan layar struk digital dari galeri.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )

                    if (cameraBindError == null) {
                        Button(
                            onClick = {
                                onExternalFlowActive(true)
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bento.accentTile,
                                contentColor = bento.onAccentTile,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("receipt_request_camera_button"),
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Izinkan Kamera", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onPickFromGallery,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("receipt_gallery_fallback_button"),
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Struk dari Galeri", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onOpenManualTransaction,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.9f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("receipt_manual_fallback_button"),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Catat Manual Tanpa Foto")
                    }
                }
            }

            // Frosted Top Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .testTag("receipt_capture_close")
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup Scanner",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Tutup",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = null,
                            tint = bento.accentTile,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Scan Struk",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                if (hasCameraPermission && cameraBindError == null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (flashEnabled) bento.accentTile else Color.Black.copy(alpha = 0.62f),
                        border = BorderStroke(
                            1.dp,
                            if (flashEnabled) Color.Transparent else Color.White.copy(alpha = 0.16f),
                        ),
                        modifier = Modifier
                            .testTag("receipt_flash_toggle")
                            .clip(RoundedCornerShape(50))
                            .clickable { flashEnabled = !flashEnabled },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = "Lampu Kilat",
                                tint = if (flashEnabled) bento.onAccentTile else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (flashEnabled) "Flash" else "Kilat",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (flashEnabled) bento.onAccentTile else Color.White,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(74.dp))
                }
            }

            // Sleek Bottom Camera & Action Dock
            if (hasCameraPermission && cameraBindError == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color(0xFF0D1117).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Zoom pill switcher
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.09f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(1f to "1x", 1.5f to "1.5x", 2f to "2x").forEach { (ratio, label) ->
                                    val selected = zoomRatio == ratio
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) bento.accentTile else Color.Transparent)
                                            .clickable { zoomRatio = ratio }
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) bento.onAccentTile else Color.White.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                            }
                        }

                        // Balanced 3-Column Dock: Galeri | Shutter | Manual
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("receipt_gallery_button")
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(enabled = !isCapturing, onClick = onPickFromGallery),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoLibrary,
                                        contentDescription = "Upload Galeri",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Galeri",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                            }

                            // Double-ring Bento Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .testTag("receipt_shutter_button")
                                    .clip(CircleShape)
                                    .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCapturing) Color.White.copy(alpha = 0.2f) else bento.accentTile)
                                    .clickable(enabled = !isCapturing) {
                                        val capture = imageCapture ?: return@clickable
                                        isCapturing = true
                                        val tempFile = File(
                                            context.cacheDir,
                                            "receipt_capture_${System.currentTimeMillis()}.jpg",
                                        )
                                        val output = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                                        capture.takePicture(
                                            output,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    scope.launch {
                                                        try {
                                                            val bmp = ReceiptImageProcessor.loadPreparedReceiptBitmapFromFile(tempFile)
                                                            if (bmp != null) {
                                                                onBitmapCaptured(bmp)
                                                            } else {
                                                                onShowError("Gagal memproses foto struk.")
                                                            }
                                                        } finally {
                                                            runCatching { tempFile.delete() }
                                                            isCapturing = false
                                                        }
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    runCatching { tempFile.delete() }
                                                    isCapturing = false
                                                    onShowError(exception.localizedMessage ?: "Gagal memotret struk.")
                                                }
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCapturing) {
                                    SkeletonBox(
                                        modifier = Modifier.size(34.dp),
                                        cornerRadius = 17.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Potret Struk",
                                        tint = bento.onAccentTile,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("receipt_manual_button")
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(enabled = !isCapturing, onClick = onOpenManualTransaction),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Catat Manual",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Manual",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
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
private fun ReceiptAnalyzingDialog(
    previewBitmap: Bitmap,
    onCancel: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onCancel)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = bento.cardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Foto Struk Sedang Dipindai",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Membaca & Memverifikasi Struk...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Menghitung ulang rincian item, pajak, dan total agar 100% akurat",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                        ReceiptScanSkeleton()
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Batal & Foto Ulang")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptErrorDialog(
    previewBitmap: Bitmap?,
    errorMessage: String,
    onRetryAi: () -> Unit,
    onManualFill: () -> Unit,
    onRetake: () -> Unit,
    onDismiss: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors

    BentoModalSheetDialog(
        title = "Hasil Scan Belum Terbaca",
        subtitle = "Kamu bisa coba pindai ulang atau isi manual langsung dari foto",
        headerIcon = Icons.Rounded.Warning,
        closeButtonTestTag = "receipt_error_close",
        onDismiss = onDismiss,
        footer = {
            if (previewBitmap != null) {
                Button(
                    onClick = onManualFill,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Isi Manual dari Foto Struk", fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Ganti Foto")
                }
                Button(
                    onClick = onRetryAi,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Coba Scan Lagi")
                }
            }
        },
    ) {
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Foto Struk",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bento.expenseColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiptPreviewEditorSheet(
    previewBitmap: Bitmap,
    draft: ScannedReceiptDraft,
    wallets: List<WalletEntity>,
    pockets: List<PocketEntity>,
    currencyConfig: CurrencyDisplayConfig,
    onRetake: () -> Unit,
    onPickFromGallery: () -> Unit,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        title: String,
        note: String,
        amountIdr: Long,
        type: TransactionType,
        pocketId: String?,
        walletId: String?,
        categoryKey: String,
        categoryLabel: String,
        locationName: String,
        occurredAtMillis: Long,
    ) -> Unit,
    onSaveTransfer: (
        fromWalletId: String,
        toWalletId: String,
        amountIdr: Long,
        note: String,
    ) -> Unit,
) {
    val bento = MaterialTheme.bentoColors

    var kind by rememberSaveable(draft) { mutableStateOf(draft.kind) }
    var amountText by rememberSaveable(draft, currencyConfig.code) {
        mutableStateOf(
            if (draft.amountIdr > 0L) idrToEditableInput(draft.amountIdr, currencyConfig) else ""
        )
    }
    var title by rememberSaveable(draft) { mutableStateOf(draft.title) }
    var locationName by rememberSaveable(draft) { mutableStateOf(draft.locationName) }
    var occurredAtMillis by rememberSaveable(draft) { mutableLongStateOf(draft.occurredAtMillis) }
    var note by rememberSaveable(draft) { mutableStateOf(draft.note) }
    var selectedCategoryKey by rememberSaveable(draft) { mutableStateOf(draft.categoryKey) }
    val defaultWalletId = (wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull())?.id.orEmpty()
    var selectedWalletId by rememberSaveable(draft) {
        val candidate = draft.matchedWalletId.takeIf { id -> wallets.any { it.id == id } }
        mutableStateOf(candidate ?: defaultWalletId)
    }
    var selectedFromWalletId by rememberSaveable(draft) {
        mutableStateOf(
            draft.matchedFromWalletId.ifBlank {
                (wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull())?.id.orEmpty()
            }
        )
    }
    var selectedToWalletId by rememberSaveable(draft) {
        mutableStateOf(
            draft.matchedToWalletId.ifBlank {
                wallets.firstOrNull { it.id != selectedFromWalletId }?.id.orEmpty()
            }
        )
    }
    var selectedPocketId by rememberSaveable(draft) { mutableStateOf(draft.matchedPocketId) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)

    BentoModalSheetDialog(
        title = "Preview Hasil Scan Struk",
        subtitle = "Periksa & edit nominal, nama, atau kategori sebelum disimpan",
        headerIcon = Icons.Rounded.DocumentScanner,
        closeButtonTestTag = "receipt_preview_drag_handle",
        onDismiss = onDismiss,
        footer = {
            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.expenseColor,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(0.95f).height(50.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Scan Ulang")
                }

                Button(
                    onClick = {
                        val amountIdr = editableInputToIdr(amountText, currencyConfig)
                        if (amountIdr <= 0L) {
                            validationError = "Masukkan nominal lebih dari ${currencyConfig.symbol}0."
                            return@Button
                        }

                        if (kind == ReceiptTransactionKind.TRANSFER) {
                            when {
                                wallets.size < 2 -> validationError = "Minimal 2 dompet dibutuhkan untuk pindah saldo."
                                selectedFromWalletId.isBlank() || selectedToWalletId.isBlank() ->
                                    validationError = "Pilih dompet asal dan dompet tujuan."
                                selectedFromWalletId == selectedToWalletId ->
                                    validationError = "Dompet asal dan tujuan harus berbeda."
                                else -> {
                                    val combinedNote = listOf(title.trim(), note.trim())
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                    onSaveTransfer(selectedFromWalletId, selectedToWalletId, amountIdr, combinedNote)
                                }
                            }
                        } else {
                            val resolvedWalletId = selectedWalletId.takeIf { it.isNotBlank() } ?: defaultWalletId
                            if (title.isBlank()) {
                                validationError = "Nama/judul transaksi tidak boleh kosong."
                                return@Button
                            }
                            if (wallets.isNotEmpty() && resolvedWalletId.isBlank()) {
                                validationError = "Pilih salah satu dompet."
                                return@Button
                            }
                            val catOption = pocketCategoryFor(selectedCategoryKey)
                            val txType = if (kind == ReceiptTransactionKind.INCOME) {
                                TransactionType.INCOME
                            } else {
                                TransactionType.EXPENSE
                            }
                            onSaveTransaction(
                                title.trim(),
                                note.trim(),
                                amountIdr,
                                txType,
                                selectedPocketId.takeIf { it.isNotBlank() },
                                resolvedWalletId.takeIf { it.isNotBlank() },
                                catOption.key,
                                catOption.label,
                                if (txType == TransactionType.EXPENSE) locationName.trim() else "",
                                occurredAtMillis,
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(50.dp)
                        .testTag("receipt_preview_save_button"),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Simpan Catatan", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
        // Receipt Thumbnail (tappable to zoom) + AI Audit Summary Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { showZoomDialog = true },
            shape = RoundedCornerShape(18.dp),
            color = bento.nestedCardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 96.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Preview Foto Struk",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomIn,
                            contentDescription = "Perbesar Foto",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = bento.highlightLabel,
                            modifier = Modifier.size(16.dp),
                        )
                        val confText = when (draft.confidenceLevel) {
                            "HIGH" -> "Verifikasi AI Akurat"
                            "MEDIUM" -> "Cek Ulang Angka"
                            "MANUAL" -> "Mode Isi Manual"
                            else -> "Gambar Kurang Jelas"
                        }
                        Text(
                            text = confText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (draft.confidenceLevel == "HIGH") bento.incomeColor else bento.warningColor,
                        )
                    }

                    if (draft.amountCheckReasoning.isNotBlank()) {
                        Text(
                            text = draft.amountCheckReasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (draft.originalForeignAmountText.isNotBlank()) {
                        Text(
                            text = "Terdeteksi ${draft.originalForeignAmountText} → dikonversi otomatis",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.highlightLabel,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = bento.cardSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showZoomDialog = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ZoomIn,
                                    contentDescription = null,
                                    tint = bento.mutedText,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "Perbesar",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = bento.highlightLabel.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onPickFromGallery),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    tint = bento.highlightLabel,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "Ganti Galeri",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = bento.highlightLabel,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (draft.warningNote.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bento.warningColor.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = draft.warningNote,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // 3-way Type Toggle: Pengeluaran / Pemasukan / Pindah Saldo
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Jenis Catatan dari Struk",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bento.nestedCardSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ReceiptTransactionKind.entries.forEach { option ->
                        val selected = kind == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("receipt_kind_${option.name.lowercase()}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        !selected -> Color.Transparent
                                        option == ReceiptTransactionKind.INCOME -> bento.incomeColor
                                        else -> bento.accentTile
                                    }
                                )
                                .clickable {
                                    kind = option
                                    validationError = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = when {
                                    !selected -> MaterialTheme.colorScheme.onSurface
                                    option == ReceiptTransactionKind.INCOME -> bento.onHero
                                    else -> bento.onAccentTile
                                },
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                amountText = sanitizeCurrencyInput(input, currencyConfig)
                validationError = null
            },
            label = { Text("Nominal Total (${currencyConfig.code})") },
            placeholder = { Text(if (currencyConfig.isBaseIdr) "Contoh: 45.000" else "Contoh: 25") },
            prefix = { Text("${currencyConfig.symbol} ") },
            visualTransformation = currencyTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (!currencyConfig.isBaseIdr && currencyConfig.decimalDigits > 0) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Number
                }
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_preview_amount_input"),
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it.take(80)
                validationError = null
            },
            label = {
                Text(
                    if (kind == ReceiptTransactionKind.TRANSFER) "Judul / Keterangan Transfer"
                    else "Nama Toko / Sumber Transaksi"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_preview_title_input"),
        )

        if (kind == ReceiptTransactionKind.TRANSFER) {
            if (wallets.size < 2) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bento.warningColor.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Kamu baru memiliki ${wallets.size} dompet. Tambah 1 dompet lagi untuk memindahkan saldo antar dompet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                Text(
                    text = "Dari Dompet (Asal)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = selectedFromWalletId == w.id,
                            onClick = {
                                selectedFromWalletId = w.id
                                if (selectedToWalletId == w.id) {
                                    selectedToWalletId = wallets.firstOrNull { it.id != w.id }?.id.orEmpty()
                                }
                                validationError = null
                            },
                            leadingIcon = {
                                WalletLogoBadge(w.providerCode, w.type, w.providerShortName, 18.dp)
                            },
                            label = { Text("${w.name} • ${FormatUtils.formatCompactMoney(w.balance, currencyConfig)}") },
                        )
                    }
                }

                Text(
                    text = "Ke Dompet (Tujuan)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = selectedToWalletId == w.id,
                            onClick = {
                                selectedToWalletId = w.id
                                validationError = null
                            },
                            leadingIcon = {
                                WalletLogoBadge(w.providerCode, w.type, w.providerShortName, 18.dp)
                            },
                            label = { Text("${w.name} • ${FormatUtils.formatCompactMoney(w.balance, currencyConfig)}") },
                        )
                    }
                }
            }
        } else {
            // Full Category Picker (same catalog as Tambah Kantong + Search)
            CategorySelectField(
                selectedKey = selectedCategoryKey,
                onClick = { showCategoryPicker = true },
                label = if (kind == ReceiptTransactionKind.INCOME) "Kategori Pemasukan" else "Kategori Pengeluaran",
                helperText = "Pilih atau cari dari semua kategori kantong",
                testTag = "receipt_preview_category_field",
            )

            // Date & Time picker for Income & Expense
            DateTimePickerRow(
                occurredAtMillis = occurredAtMillis,
                onDateTimeChange = { occurredAtMillis = it },
            )

            // Place Name input ONLY for Expense
            if (kind == ReceiptTransactionKind.EXPENSE) {
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it.take(100) },
                    label = { Text("Nama Tempat / Merchant") },
                    placeholder = { Text("Contoh: Indomaret, Kopi Kenangan, SPBU") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            tint = bento.highlightLabel,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_preview_location_input"),
                )
            }

            if (wallets.isNotEmpty()) {
                Text(
                    text = "Pilih Salah Satu Dompet *",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    wallets.forEach { w ->
                        val defaultSuffix = if (w.isDefault) " • Utama" else ""
                        FilterChip(
                            selected = selectedWalletId == w.id,
                            onClick = {
                                selectedWalletId = w.id
                                validationError = null
                            },
                            leadingIcon = {
                                WalletLogoBadge(w.providerCode, w.type, w.providerShortName, 18.dp)
                            },
                            label = {
                                Text("${w.name}$defaultSuffix (${FormatUtils.formatCompactMoney(w.balance, currencyConfig)})")
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (kind == ReceiptTransactionKind.EXPENSE) {
                        "Diambil dari Kantong Mana?"
                    } else {
                        "Ditambahkan ke Kantong Mana?"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedPocketId.isBlank(),
                        onClick = { selectedPocketId = "" },
                        label = { Text("Tidak Ada di Kantong (Riwayat Saja)") },
                    )
                    pockets.forEach { pocket ->
                        val hint = if (pocket.pocketMode == PocketMode.BUDGET_LIMIT) {
                            "Sisa ${FormatUtils.formatCompactMoney(pocket.remainingAmount, currencyConfig)}"
                        } else {
                            "${FormatUtils.formatCompactMoney(pocket.currentAmount, currencyConfig)}/${FormatUtils.formatCompactMoney(pocket.targetAmount, currencyConfig)}"
                        }
                        FilterChip(
                            selected = selectedPocketId == pocket.id,
                            onClick = { selectedPocketId = pocket.id },
                            label = { Text("${pocket.name} ($hint)") },
                        )
                    }
                }
                Text(
                    text = if (selectedPocketId.isNotBlank()) {
                        "Alokasi kantong terpilih ikut diperbarui bersama saldo Dompet Utama."
                    } else {
                        "Jika tidak ada di kantong, transaksi hanya masuk ke riwayat & saldo Dompet Utama."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = bento.mutedText,
                )
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(500) },
            label = { Text("Rincian Item / Nomor Referensi Struk") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_preview_note_input"),
        )
    }

    if (showCategoryPicker) {
        CategorySearchPickerDialog(
            selectedKey = selectedCategoryKey,
            title = "Pilih Kategori Transaksi",
            onDismiss = { showCategoryPicker = false },
            onSelect = { chosen ->
                selectedCategoryKey = chosen.key
                val match = pockets.firstOrNull { it.iconKey.equals(chosen.key, ignoreCase = true) }
                if (match != null) {
                    selectedPocketId = match.id
                }
                showCategoryPicker = false
            },
        )
    }

    if (showZoomDialog) {
        ZoomableReceiptImageDialog(
            bitmap = previewBitmap,
            onDismiss = { showZoomDialog = false },
        )
    }
}

@Composable
private fun ZoomableReceiptImageDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto Struk Diperbesar",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = nextScale
                            if (nextScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    ),
            )

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup Perbesaran",
                        tint = Color.White,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Cubit untuk memperbesar / menggeser foto struk",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}
