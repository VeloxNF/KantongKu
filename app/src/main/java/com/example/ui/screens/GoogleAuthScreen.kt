package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.cloud.GoogleAuthHelper
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.SkeletonBox
import com.example.ui.theme.bentoColors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoogleAuthScreen(
    isOnline: Boolean,
    externalError: String?,
    onRefreshConnectivity: () -> Boolean,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val displayError = localError ?: externalError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Bento Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(bento.heroStart, bento.heroEnd)))
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(bento.accentTile),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = stringResource(R.string.app_name),
                                tint = bento.onAccentTile,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = bento.onHero,
                            )
                            Text(
                                text = "Pengatur Keuangan Pribadi Cerdas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bento.onHero.copy(alpha = 0.84f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Semua dompet bank, e-wallet, kantong anggaran, hingga kurs dunia tersusun rapi dalam satu aplikasi ringan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = bento.onHero.copy(alpha = 0.92f),
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HeroMetricChip("Bank & E-Wallet ID")
                        HeroMetricChip("Kantong Pintar")
                        HeroMetricChip("Kurs 160+ Negara")
                    }
                }
            }

            // Bento Feature Advantages Grid
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = "Kenapa Memilih KantongKu?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dirancang praktis untuk bantu jaga arus kas dan target tabungan harian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = bento.mutedText,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdvantageTileCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Multidompet Lengkap",
                        description = "Tunai, GoPay, OVO, DANA, BRI, BCA, Mandiri, Jago & ratusan bank Indonesia.",
                        accentColor = bento.pocketColors.getOrElse(0) { bento.highlightLabel },
                        modifier = Modifier.weight(1f),
                    )
                    AdvantageTileCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Kantong Pintar",
                        description = "Bagi anggaran otomatis per hari, minggu, atau bulan sesuai gajian Anda.",
                        accentColor = bento.pocketColors.getOrElse(1) { bento.highlightLabel },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdvantageTileCard(
                        icon = Icons.Rounded.BarChart,
                        title = "Grafik & Peringatan",
                        description = "Bandingkan target vs pengeluaran bulanan plus notifikasi batas kategori.",
                        accentColor = bento.pocketColors.getOrElse(2) { bento.highlightLabel },
                        modifier = Modifier.weight(1f),
                    )
                    AdvantageTileCard(
                        icon = Icons.Rounded.CurrencyExchange,
                        title = "Kurs Mata Uang Live",
                        description = "Switch mata uang dunia otomatis lengkap dengan bendera & format angka.",
                        accentColor = bento.pocketColors.getOrElse(4) { bento.incomeColor },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bento.nestedCardSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(bento.iconBadgeBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                tint = bento.highlightLabel,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aman Biometrik, Jalan Offline & Sinkron Cloud",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Kunci sidik jari/wajah, tetap lancar mencatat tanpa sinyal, dan tersinkron otomatis saat online.",
                                style = MaterialTheme.typography.labelSmall,
                                color = bento.mutedText,
                            )
                        }
                    }
                }
            }

            // Sign-In Action Card
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_connectivity_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) bento.incomeColor.copy(alpha = 0.16f)
                                else bento.warningColor.copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                            contentDescription = if (isOnline) "Online" else "Offline",
                            tint = if (isOnline) bento.incomeColor else bento.warningColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = if (isOnline) {
                            "Internet aktif • Siap masuk dan simpan data ke cloud"
                        } else {
                            "Sedang offline • Aktifkan internet untuk masuk pertama kali"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOnline) bento.incomeColor else bento.warningColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bento.expenseColor.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = bento.expenseColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = displayError,
                                style = MaterialTheme.typography.bodySmall,
                                color = bento.expenseColor,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        localError = null
                        val connected = onRefreshConnectivity()
                        if (!connected) {
                            val msg = "Koneksi internet dibutuhkan untuk Masuk dengan Google. Silakan nyalakan Wi-Fi atau data seluler."
                            localError = msg
                            onAuthError(msg)
                            return@Button
                        }
                        isLoading = true
                        GoogleAuthHelper.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                localError = null
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                localError = msg
                                onAuthError(msg)
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            },
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                ) {
                    if (isLoading) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(18.dp),
                            cornerRadius = 9.dp,
                        )
                    } else {
                        GoogleGMark(
                            color = bento.onAccentTile,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = stringResource(R.string.sign_in_google),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricChip(text: String) {
    val bento = MaterialTheme.bentoColors
    Surface(
        shape = RoundedCornerShape(50),
        color = bento.heroTrack,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = bento.onHero,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun AdvantageTileCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bento.nestedCardSurface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = bento.mutedText,
            )
        }
    }
}

@Composable
private fun GoogleGMark(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f
        val outerR = r * 0.94f
        val innerR = r * 0.54f
        val barTop = cy - r * 0.16f
        val barBot = cy + r * 0.18f

        val outerRect = Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
        val innerRect = Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR)

        val startDeg = -9.8f
        val endDeg = 318f
        val innerStartDeg = 19.5f

        val topStartRad = Math.toRadians(startDeg.toDouble())
        val topEndX = cx + (outerR * cos(topStartRad)).toFloat()

        val gPath = Path().apply {
            moveTo(cx, barTop)
            lineTo(topEndX, barTop)
            arcTo(
                rect = outerRect,
                startAngleDegrees = startDeg,
                sweepAngleDegrees = endDeg - startDeg,
                forceMoveTo = false,
            )
            val cutRad = Math.toRadians(endDeg.toDouble())
            lineTo(
                cx + (innerR * cos(cutRad)).toFloat(),
                cy + (innerR * sin(cutRad)).toFloat(),
            )
            arcTo(
                rect = innerRect,
                startAngleDegrees = endDeg,
                sweepAngleDegrees = -(endDeg - innerStartDeg),
                forceMoveTo = false,
            )
            lineTo(cx, barBot)
            close()
        }

        drawPath(path = gPath, color = color)
    }
}
