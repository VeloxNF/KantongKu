package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.bentoColors

@Composable
fun rememberShimmerBrush(): Brush {
    val bento = MaterialTheme.bentoColors
    val baseColor = bento.nestedCardSurface
    val highlightColor = bento.borderSubtle.copy(alpha = if (bento.isDark) 0.45f else 0.72f)
    val transition = rememberInfiniteTransition(label = "bento_skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim - 280f, y = translateAnim - 280f),
        end = Offset(x = translateAnim + 280f, y = translateAnim + 280f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    shimmerBrush: Brush = rememberShimmerBrush(),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush)
    )
}

@Composable
fun HomeDashboardSkeleton(
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val brush = rememberShimmerBrush()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("home_dashboard_skeleton"),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth >= 680.dp
        Column(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Greeting + Action Pills Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(46.dp),
                        cornerRadius = 14.dp,
                        shimmerBrush = brush,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBox(
                            modifier = Modifier.width(96.dp).height(12.dp),
                            cornerRadius = 6.dp,
                            shimmerBrush = brush,
                        )
                        SkeletonBox(
                            modifier = Modifier.width(150.dp).height(18.dp),
                            cornerRadius = 8.dp,
                            shimmerBrush = brush,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(
                        modifier = Modifier.width(68.dp).height(38.dp),
                        cornerRadius = 14.dp,
                        shimmerBrush = brush,
                    )
                    SkeletonBox(
                        modifier = Modifier.size(38.dp),
                        cornerRadius = 19.dp,
                        shimmerBrush = brush,
                    )
                }
            }

            // Hero Card + Quick Actions Skeleton
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeroSkeletonCard(brush = brush, modifier = Modifier.weight(1.15f))
                    QuickActionsSkeletonGrid(brush = brush, modifier = Modifier.weight(1f))
                }
            } else {
                HeroSkeletonCard(brush = brush, modifier = Modifier.fillMaxWidth())
                QuickActionsSkeletonGrid(brush = brush, modifier = Modifier.fillMaxWidth())
            }

            // Scan Struk Banner Skeleton
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = bento.cardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.55f)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBox(modifier = Modifier.size(44.dp), cornerRadius = 14.dp, shimmerBrush = brush)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.55f).height(16.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.85f).height(12.dp), cornerRadius = 6.dp, shimmerBrush = brush)
                    }
                }
            }

            // Multidompet Section Skeleton
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = bento.cardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.55f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(modifier = Modifier.width(120.dp).height(18.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                        SkeletonBox(modifier = Modifier.width(78.dp).height(30.dp), cornerRadius = 10.dp, shimmerBrush = brush)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        repeat(2) {
                            Surface(
                                modifier = Modifier.width(154.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = bento.nestedCardSurface,
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SkeletonBox(modifier = Modifier.size(34.dp), cornerRadius = 10.dp, shimmerBrush = brush)
                                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.75f).height(14.dp), cornerRadius = 7.dp, shimmerBrush = brush)
                                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.90f).height(16.dp), cornerRadius = 7.dp, shimmerBrush = brush)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions Skeleton Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = bento.cardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.55f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBox(modifier = Modifier.width(145.dp).height(18.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                    repeat(3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SkeletonBox(modifier = Modifier.size(40.dp), cornerRadius = 12.dp, shimmerBrush = brush)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                SkeletonBox(modifier = Modifier.fillMaxWidth(0.60f).height(14.dp), cornerRadius = 7.dp, shimmerBrush = brush)
                                SkeletonBox(modifier = Modifier.fillMaxWidth(0.40f).height(11.dp), cornerRadius = 5.dp, shimmerBrush = brush)
                            }
                            SkeletonBox(modifier = Modifier.width(74.dp).height(16.dp), cornerRadius = 7.dp, shimmerBrush = brush)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSkeletonCard(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp),
            shape = RoundedCornerShape(26.dp),
            color = bento.cardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SkeletonBox(modifier = Modifier.width(150.dp).height(16.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                    SkeletonBox(modifier = Modifier.width(66.dp).height(26.dp), cornerRadius = 13.dp, shimmerBrush = brush)
                }
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.70f).height(34.dp), cornerRadius = 10.dp, shimmerBrush = brush)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SkeletonBox(modifier = Modifier.width(105.dp).height(26.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                    SkeletonBox(modifier = Modifier.width(105.dp).height(26.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(modifier = Modifier.width(120.dp).height(24.dp), cornerRadius = 12.dp, shimmerBrush = brush)
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonBox(modifier = Modifier.width(110.dp).height(24.dp), cornerRadius = 12.dp, shimmerBrush = brush)
        }
    }
}

@Composable
private fun QuickActionsSkeletonGrid(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) {
                    SkeletonBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp),
                        cornerRadius = 22.dp,
                        shimmerBrush = brush,
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptScanSkeleton(
    modifier: Modifier = Modifier,
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("receipt_scan_skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(modifier = Modifier.weight(1f).height(32.dp), cornerRadius = 10.dp, shimmerBrush = brush)
            SkeletonBox(modifier = Modifier.weight(1f).height(32.dp), cornerRadius = 10.dp, shimmerBrush = brush)
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.75f).height(18.dp), cornerRadius = 8.dp, shimmerBrush = brush)
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(42.dp), cornerRadius = 14.dp, shimmerBrush = brush)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(modifier = Modifier.weight(1f).height(36.dp), cornerRadius = 12.dp, shimmerBrush = brush)
            SkeletonBox(modifier = Modifier.weight(1f).height(36.dp), cornerRadius = 12.dp, shimmerBrush = brush)
        }
    }
}

@Composable
fun CurrencyRatesListSkeleton(
    rowCount: Int = 5,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("currency_rates_skeleton"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(rowCount) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = bento.nestedCardSurface,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBox(modifier = Modifier.size(36.dp), cornerRadius = 18.dp, shimmerBrush = brush)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp), cornerRadius = 7.dp, shimmerBrush = brush)
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.38f).height(11.dp), cornerRadius = 5.dp, shimmerBrush = brush)
                    }
                    SkeletonBox(modifier = Modifier.width(64.dp).height(16.dp), cornerRadius = 8.dp, shimmerBrush = brush)
                }
            }
        }
    }
}

@Composable
fun SimpleRowsSkeleton(
    rowCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(rowCount) { index ->
            val widthFraction = when (index % 3) {
                0 -> 0.88f
                1 -> 0.65f
                else -> 0.48f
            }
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(14.dp),
                cornerRadius = 7.dp,
                shimmerBrush = brush,
            )
        }
    }
}
