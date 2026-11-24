package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WalletType
import com.example.data.wallet.IndonesianWalletCatalog
import com.example.data.wallet.WalletLogoMotif
import com.example.ui.theme.bentoColors

/**
 * Renders an authentic, distinctive brand logo badge for each Indonesian Bank,
 * Indonesian E-Wallet, and Physical Cash (Uang Tunai).
 */
@Composable
fun WalletLogoBadge(
    providerCode: String,
    walletType: WalletType,
    shortName: String = "",
    size: Dp = 38.dp,
    modifier: Modifier = Modifier,
) {
    val style = remember(providerCode, walletType, shortName) {
        IndonesianWalletCatalog.brandStyleFor(providerCode, walletType, shortName)
    }
    val primaryColor = Color(style.primaryHex)
    val accentColor = Color(style.accentHex)
    val onPrimaryColor = Color(style.onPrimaryHex)
    val cornerRadius = (size.value * 0.28f).dp
    val isTiny = size < 22.dp

    if (style.motif == WalletLogoMotif.CASH_BANKNOTE || walletType == WalletType.CASH) {
        CashBanknoteLogo(
            primaryColor = primaryColor,
            goldColor = accentColor,
            size = size,
            cornerRadius = cornerRadius,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.88f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            when (style.motif) {
                WalletLogoMotif.BRI_STRIPE -> {
                    // Horizontal dynamic speed bars at top-right
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.45f, h * 0.22f),
                        end = Offset(w * 0.86f, h * 0.22f),
                        strokeWidth = h * 0.08f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = onPrimaryColor.copy(alpha = 0.65f),
                        start = Offset(w * 0.62f, h * 0.80f),
                        end = Offset(w * 0.86f, h * 0.80f),
                        strokeWidth = h * 0.065f,
                        cap = StrokeCap.Round,
                    )
                }

                WalletLogoMotif.MANDIRI_RIBBON -> {
                    // Flowing golden wave ribbon across top
                    val path = Path().apply {
                        moveTo(0f, h * 0.26f)
                        quadraticTo(w * 0.45f, h * 0.08f, w, h * 0.28f)
                        lineTo(w, 0f)
                        lineTo(0f, 0f)
                        close()
                    }
                    drawPath(path, color = accentColor)
                }

                WalletLogoMotif.BNI_ACCENT -> {
                    // Orange diagonal corner wave
                    val path = Path().apply {
                        moveTo(w * 0.58f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h * 0.42f)
                        close()
                    }
                    drawPath(path, color = accentColor)
                }

                WalletLogoMotif.BTN_ROOF -> {
                    // Architectural chevron roof bar along top edge
                    val roof = Path().apply {
                        moveTo(w * 0.14f, h * 0.26f)
                        lineTo(w * 0.50f, h * 0.10f)
                        lineTo(w * 0.86f, h * 0.26f)
                    }
                    drawPath(
                        path = roof,
                        color = accentColor,
                        style = Stroke(width = h * 0.08f, cap = StrokeCap.Round),
                    )
                }

                WalletLogoMotif.BSI_CRESCENT -> {
                    // Five-point style star dot at top-right + subtle crescent glow
                    drawCircle(
                        color = accentColor,
                        radius = w * 0.11f,
                        center = Offset(w * 0.78f, h * 0.22f),
                    )
                    drawCircle(
                        color = onPrimaryColor.copy(alpha = 0.22f),
                        radius = w * 0.34f,
                        center = Offset(w * 0.18f, h * 0.85f),
                    )
                }

                WalletLogoMotif.BCA_WING -> {
                    // Subtle golden diagonal ribbon accent
                    val wing = Path().apply {
                        moveTo(0f, h * 0.80f)
                        quadraticTo(w * 0.45f, h * 0.94f, w, h * 0.72f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(wing, color = accentColor.copy(alpha = 0.9f))
                }

                WalletLogoMotif.JAGO_SUN -> {
                    // Signature Bank Jago bright sun disc with deep plum inner arc
                    drawCircle(
                        color = accentColor.copy(alpha = 0.14f),
                        radius = w * 0.42f,
                        center = Offset(w * 0.5f, h * 0.5f),
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.24f, h * 0.52f),
                        size = Size(w * 0.52f, h * 0.28f),
                        style = Stroke(width = h * 0.07f, cap = StrokeCap.Round),
                    )
                }

                WalletLogoMotif.SEABANK_WAVE -> {
                    val wave = Path().apply {
                        moveTo(0f, h * 0.76f)
                        quadraticTo(w * 0.3f, h * 0.62f, w * 0.6f, h * 0.76f)
                        quadraticTo(w * 0.82f, h * 0.86f, w, h * 0.72f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(wave, color = accentColor.copy(alpha = 0.65f))
                }

                WalletLogoMotif.BLU_DROPLET -> {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.45f),
                        radius = w * 0.18f,
                        center = Offset(w * 0.76f, h * 0.24f),
                    )
                }

                WalletLogoMotif.JENIUS_SPARK -> {
                    drawCircle(
                        color = accentColor,
                        radius = w * 0.13f,
                        center = Offset(w * 0.80f, h * 0.20f),
                    )
                }

                WalletLogoMotif.NEOBANK_COIN -> {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.14f),
                        topLeft = Offset(w * 0.08f, h * 0.08f),
                        size = Size(w * 0.84f, h * 0.84f),
                        cornerRadius = CornerRadius(w * 0.2f, w * 0.2f),
                        style = Stroke(width = h * 0.06f),
                    )
                }

                WalletLogoMotif.CIMB_DIAMOND -> {
                    // Signature CIMB red octagonal diamond ribbon on bottom strip
                    val diamond = Path().apply {
                        moveTo(w * 0.5f, h * 0.72f)
                        lineTo(w * 0.66f, h * 0.84f)
                        lineTo(w * 0.5f, h * 0.96f)
                        lineTo(w * 0.34f, h * 0.84f)
                        close()
                    }
                    drawPath(diamond, color = accentColor)
                }

                WalletLogoMotif.PERMATA_GEM -> {
                    val gem = Path().apply {
                        moveTo(w * 0.75f, h * 0.10f)
                        lineTo(w * 0.90f, h * 0.24f)
                        lineTo(w * 0.75f, h * 0.38f)
                        lineTo(w * 0.60f, h * 0.24f)
                        close()
                    }
                    drawPath(gem, color = accentColor)
                }

                WalletLogoMotif.HSBC_HEXAGON -> {
                    // Classic red/white bowtie triangles on top edge
                    val leftTri = Path().apply {
                        moveTo(w * 0.28f, h * 0.10f)
                        lineTo(w * 0.50f, h * 0.24f)
                        lineTo(w * 0.28f, h * 0.36f)
                        close()
                    }
                    val rightTri = Path().apply {
                        moveTo(w * 0.72f, h * 0.10f)
                        lineTo(w * 0.50f, h * 0.24f)
                        lineTo(w * 0.72f, h * 0.36f)
                        close()
                    }
                    drawPath(leftTri, color = accentColor.copy(alpha = 0.9f))
                    drawPath(rightTri, color = accentColor.copy(alpha = 0.9f))
                }

                WalletLogoMotif.UOB_BARS -> {
                    // Five vertical red bars at top
                    val barW = w * 0.08f
                    val gap = w * 0.06f
                    val startX = w * 0.20f
                    for (i in 0 until 5) {
                        drawRect(
                            color = accentColor,
                            topLeft = Offset(startX + i * (barW + gap), 0f),
                            size = Size(barW, h * 0.22f),
                        )
                    }
                }

                WalletLogoMotif.CITIBANK_ARC -> {
                    drawArc(
                        color = accentColor,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.18f, h * 0.12f),
                        size = Size(w * 0.64f, h * 0.32f),
                        style = Stroke(width = h * 0.08f, cap = StrokeCap.Round),
                    )
                }

                WalletLogoMotif.STANCHART_HELIX -> {
                    drawCircle(
                        color = accentColor,
                        radius = w * 0.18f,
                        center = Offset(w * 0.80f, h * 0.82f),
                    )
                }

                WalletLogoMotif.OCBC_COMPASS -> {
                    drawCircle(
                        color = onPrimaryColor.copy(alpha = 0.24f),
                        radius = w * 0.38f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = h * 0.06f),
                    )
                }

                WalletLogoMotif.GOPAY_WALLET -> {
                    // Sleek wallet flap circle cutout top-right
                    drawRoundRect(
                        color = onPrimaryColor.copy(alpha = 0.18f),
                        topLeft = Offset(w * 0.12f, h * 0.14f),
                        size = Size(w * 0.76f, h * 0.72f),
                        cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                    )
                    drawCircle(
                        color = onPrimaryColor,
                        radius = w * 0.09f,
                        center = Offset(w * 0.78f, h * 0.24f),
                    )
                }

                WalletLogoMotif.OVO_RING -> {
                    // Concentric luminous ring frame
                    drawCircle(
                        color = accentColor.copy(alpha = 0.55f),
                        radius = w * 0.40f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = h * 0.075f),
                    )
                }

                WalletLogoMotif.DANA_RIBBON -> {
                    // Folded azure wave card banner
                    val ribbon = Path().apply {
                        moveTo(0f, h * 0.78f)
                        quadraticTo(w * 0.35f, h * 0.65f, w, h * 0.82f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(ribbon, color = accentColor.copy(alpha = 0.85f))
                }

                WalletLogoMotif.SHOPEEPAY_BAG -> {
                    // Bag handle top loop
                    drawArc(
                        color = onPrimaryColor.copy(alpha = 0.85f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.32f, h * 0.08f),
                        size = Size(w * 0.36f, h * 0.26f),
                        style = Stroke(width = h * 0.07f, cap = StrokeCap.Round),
                    )
                }

                WalletLogoMotif.LINKAJA_BADGE -> {
                    val corner = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.35f, 0f)
                        lineTo(0f, h * 0.35f)
                        close()
                    }
                    drawPath(corner, color = accentColor.copy(alpha = 0.5f))
                }

                WalletLogoMotif.EMONEY_CHIP -> {
                    // Contactless smartcard chip + NFC arcs
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(w * 0.14f, h * 0.16f),
                        size = Size(w * 0.24f, h * 0.18f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                    )
                    drawArc(
                        color = onPrimaryColor.copy(alpha = 0.75f),
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * 0.64f, h * 0.14f),
                        size = Size(w * 0.22f, h * 0.22f),
                        style = Stroke(width = h * 0.06f, cap = StrokeCap.Round),
                    )
                }

                WalletLogoMotif.BANK_ARCH -> {
                    // Minimal architectural top bar & pillar dot
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.20f, h * 0.16f),
                        end = Offset(w * 0.80f, h * 0.16f),
                        strokeWidth = h * 0.07f,
                        cap = StrokeCap.Round,
                    )
                }

                WalletLogoMotif.EWALLET_BOLT -> {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.4f),
                        radius = w * 0.18f,
                        center = Offset(w * 0.82f, h * 0.82f),
                    )
                }

                WalletLogoMotif.CASH_BANKNOTE -> Unit
            }
        }

        val displayLabel = if (isTiny) style.badgeText.take(2) else style.badgeText
        val textLen = displayLabel.length
        val fontSizeSp = when {
            isTiny -> (size.value * 0.42f).sp
            textLen <= 2 -> (size.value * 0.36f).sp
            textLen == 3 -> (size.value * 0.31f).sp
            textLen == 4 -> (size.value * 0.25f).sp
            else -> (size.value * 0.21f).sp
        }

        Text(
            text = displayLabel,
            fontSize = fontSizeSp,
            lineHeight = fontSizeSp,
            fontWeight = FontWeight.ExtraBold,
            color = onPrimaryColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun CashBanknoteLogo(
    primaryColor: Color,
    goldColor: Color,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.84f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Banknote sheet silhouette
            val noteLeft = w * 0.13f
            val noteTop = h * 0.24f
            val noteW = w * 0.74f
            val noteH = h * 0.52f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.94f),
                topLeft = Offset(noteLeft, noteTop),
                size = Size(noteW, noteH),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                style = Stroke(width = (h * 0.075f).coerceAtLeast(1.2f)),
            )

            // Center medallion circle on banknote
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = h * 0.12f,
                center = Offset(w * 0.46f, h * 0.50f),
            )

            // Gold coin overlapping bottom-right of banknote
            drawCircle(
                color = goldColor,
                radius = w * 0.18f,
                center = Offset(w * 0.74f, h * 0.72f),
            )
            drawCircle(
                color = primaryColor,
                radius = w * 0.07f,
                center = Offset(w * 0.74f, h * 0.72f),
            )
        }
    }
}

/**
 * Compact rounded flag pill displaying a country's flag emoji + optional code badge.
 */
@Composable
fun CurrencyFlagBadge(
    flagEmoji: String,
    code: String = "",
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape((size.value * 0.28f).dp),
        color = bento.cardSurface,
        border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.55f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = flagEmoji.ifBlank { code.take(2) },
                fontSize = (size.value * 0.54f).sp,
                lineHeight = (size.value * 0.6f).sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
