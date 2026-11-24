package com.example.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.auth.BiometricAuthHelper
import com.example.data.auth.BiometricAvailability
import com.example.data.auth.findFragmentActivity
import com.example.data.local.LocalAccountEntity
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.theme.bentoColors

@Composable
fun BiometricLockScreen(
    account: LocalAccountEntity,
    onUnlocked: () -> Unit,
    onFallbackToPinLogin: () -> Unit,
    onPromptVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }

    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)
    val promptNegative = stringResource(R.string.biometric_prompt_negative)

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    fun launchBiometricPrompt() {
        val host = activity
        if (host == null) {
            isErrorStatus = true
            statusMessage = "Layar biometrik tidak tersedia. Keluar untuk masuk ulang dengan Google."
            return
        }
        statusMessage = null
        isErrorStatus = false
        BiometricAuthHelper.authenticate(
            activity = host,
            title = promptTitle,
            subtitle = promptSubtitle,
            negativeButtonText = promptNegative,
            onPromptVisibilityChanged = onPromptVisibilityChanged,
            onSuccess = {
                statusMessage = null
                onUnlocked()
            },
            onError = { errorCode, message ->
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onFallbackToPinLogin()
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> {
                        isErrorStatus = false
                        statusMessage = "Verifikasi dibatalkan. Ketuk tombol di bawah untuk mencoba lagi."
                    }
                    else -> {
                        isErrorStatus = true
                        statusMessage = message.ifBlank { "Autentikasi biometrik gagal. Coba lagi atau keluar akun." }
                    }
                }
            },
            onFailedAttempt = {
                isErrorStatus = true
                statusMessage = "Sidik jari atau wajah tidak dikenali. Silakan coba lagi."
            },
        )
    }

    LaunchedEffect(account.id) {
        launchBiometricPrompt()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Bento Hero Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(bento.heroStart, bento.heroEnd)))
                    .padding(22.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bento.accentTile),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Kunci Biometrik",
                            tint = bento.onAccentTile,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "KantongKu Terkunci",
                        style = MaterialTheme.typography.headlineMedium,
                        color = bento.onHero,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Halo, ${account.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = bento.onHero.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Saldo, kantong target, dan catatan keuangan Anda dilindungi kunci biometrik.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = bento.onHero.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Action Bento Card
            BentoSurfaceCard(
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bento.nestedCardSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = bento.highlightLabel,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = "Verifikasi Sidik Jari / Wajah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bento.nestedCardSurface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isErrorStatus) bento.expenseColor else bento.mutedText,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = ::launchBiometricPrompt,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("biometric_unlock_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = " Buka dengan Biometrik",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onFallbackToPinLogin,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("biometric_use_pin_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = " Keluar & Masuk Google",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricSecurityCard(
    biometricLockEnabled: Boolean,
    availability: BiometricAvailability,
    onToggleBiometricLock: (Boolean) -> Unit,
    onOpenEnrollSettings: () -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors

    val statusDescription = when (availability) {
        BiometricAvailability.AVAILABLE -> if (biometricLockEnabled) {
            "Aktif • Aplikasi meminta sidik jari / wajah saat dibuka"
        } else {
            "Nonaktif • Perangkat siap mengaktifkan kunci biometrik"
        }
        BiometricAvailability.NOT_ENROLLED -> "Belum ada sidik jari, wajah, atau kunci layar terdaftar di HP ini"
        BiometricAvailability.HARDWARE_UNAVAILABLE -> "Sensor biometrik sedang tidak tersedia sementara"
        BiometricAvailability.UNSUPPORTED -> "Perangkat ini tidak mendukung autentikasi biometrik"
    }

    BentoSurfaceCard(
        contentPadding = PaddingValues(18.dp),
        modifier = modifier.testTag("biometric_security_card"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = bento.highlightLabel,
            )
            Text(
                text = "Keamanan & Biometrik",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = statusDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = if (availability == BiometricAvailability.NOT_ENROLLED) bento.warningColor else bento.mutedText,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Kunci Sidik Jari / Wajah",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Lindungi saldo & catatan saat membuka aplikasi",
                        style = MaterialTheme.typography.labelMedium,
                        color = bento.mutedText,
                    )
                }
                Switch(
                    checked = biometricLockEnabled && availability.isReady,
                    enabled = availability.isReady,
                    onCheckedChange = onToggleBiometricLock,
                    modifier = Modifier.testTag("biometric_lock_switch"),
                )
            }
        }

        if (availability == BiometricAvailability.NOT_ENROLLED) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenEnrollSettings,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("biometric_enroll_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(" Atur Biometrik di Pengaturan HP")
            }
        } else if (availability.isReady && biometricLockEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onLockNow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("lock_app_now_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(" Kunci Aplikasi Sekarang")
            }
        }
    }
}
