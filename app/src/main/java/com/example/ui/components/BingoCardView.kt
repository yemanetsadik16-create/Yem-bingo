package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BingoCard
import com.example.model.BingoCell
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BlueHeader
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.PurpleHeader
import com.example.ui.theme.RedHeader
import com.example.ui.theme.TextPrimary

@Composable
fun BingoCardView(
    card: BingoCard,
    isInteractive: Boolean,
    onCellClick: (row: Int, col: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "winning_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.5.dp, GoldPrimary, RoundedCornerShape(18.dp))
            .padding(10.dp)
            .testTag("bingo_card_container")
    ) {
        // B - I - N - G - O Header Row
        val headerLetters = listOf("B", "I", "N", "G", "O")
        val headerColors = listOf(RedHeader, BlueHeader, GoldPrimary, MintPrimary, PurpleHeader)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(RedHeader, BlueHeader, GoldPrimary, MintPrimary, PurpleHeader)
                    )
                )
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            headerLetters.forEachIndexed { index, letter ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5x5 Grid
        card.grid.forEachIndexed { r, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEachIndexed { c, cell ->
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        BingoCellView(
                            cell = cell,
                            pulseScale = pulseScale,
                            isInteractive = isInteractive,
                            onClick = { onCellClick(r, c) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BingoCellView(
    cell: BingoCell,
    pulseScale: Float,
    isInteractive: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)

    val backgroundBrush = when {
        cell.isWinning -> Brush.radialGradient(
            listOf(GoldPrimary, OrangeDynamic)
        )
        cell.isFree -> Brush.radialGradient(
            listOf(GoldLight, GoldDark)
        )
        cell.isMarked -> Brush.radialGradient(
            listOf(MintPrimary, MintDark)
        )
        else -> Brush.verticalGradient(
            listOf(BgSurface, BgSurface)
        )
    }

    val borderColor = when {
        cell.isWinning -> Color.White
        cell.isFree -> GoldLight
        cell.isMarked -> MintLight
        else -> BorderColor
    }

    val textColor = when {
        cell.isWinning || cell.isFree || cell.isMarked -> BgDark
        else -> TextPrimary
    }

    val scaleModifier = if (cell.isWinning) Modifier.scale(pulseScale) else Modifier

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(scaleModifier)
            .clip(shape)
            .background(backgroundBrush)
            .border(width = if (cell.isWinning) 2.dp else 1.dp, color = borderColor, shape = shape)
            .then(
                if (isInteractive) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell.isFree) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "★",
                    color = BgDark,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = "FREE",
                    color = BgDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp
                )
            }
        } else {
            Text(
                text = cell.number.toString(),
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}
