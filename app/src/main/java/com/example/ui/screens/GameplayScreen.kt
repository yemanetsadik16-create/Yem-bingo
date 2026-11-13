package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BingoCard
import com.example.ui.components.BingoCardView
import com.example.ui.components.Matrix100Grid
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TextSecondary

import com.example.viewmodel.BingoViewModel

@Composable
fun GameplayScreen(
    currentCard: BingoCard?,
    calledNumbers: List<Int>,
    latestCalledBall: Int?,
    amharicAnnouncement: String? = null,
    isAutoDaub: Boolean,
    onToggleAutoDaub: () -> Unit,
    onDaubCell: (row: Int, col: Int) -> Unit,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ballLetter = latestCalledBall?.let { BingoViewModel.getLetterForBall(it) } ?: ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("gameplay_screen")
    ) {
        // Amharic Caller Man Spotlight Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCardElevated)
                .border(1.2.dp, GoldPrimary, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .testTag("caller_spotlight_strip")
        ) {
            // Caller Header Row: Avatar, Badge, and Auto-Daub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFC107)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨🏽‍💼", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "የአማርኛ ጠሪ • Amharic Caller (Fast & Clear)",
                        color = GoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                // Auto-Daub Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3320C997))
                        .border(1.dp, MintPrimary, RoundedCornerShape(8.dp))
                        .clickable { onToggleAutoDaub() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("toggle_auto_daub_button")
                ) {
                    Text(
                        text = "Auto-Daub: ${if (isAutoDaub) "ON" else "OFF"}",
                        color = MintLight,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Ball & Callout Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Caller Ball
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(GoldLight, OrangeDynamic)
                            )
                        )
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestCalledBall != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = ballLetter,
                                color = BgDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Text(
                                text = latestCalledBall.toString(),
                                color = BgDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    } else {
                        Text(
                            text = "--",
                            color = BgDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (latestCalledBall != null)
                            "BALL $ballLetter-$latestCalledBall CALLED"
                        else
                            "CALLING BALLS...",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )

                    // Spoken Amharic Callout Text
                    Text(
                        text = amharicAnnouncement ?: "🎙️ ጨዋታው እየተካሄደ ነው... (Calling live)",
                        color = MintLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )

                    Text(
                        text = "${calledNumbers.size}/75 Numbers Called • 1-75 Bingo",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Interactive Bingo Card
        if (currentCard != null) {
            BingoCardView(
                card = currentCard,
                isInteractive = !isAutoDaub, // If auto-daub is OFF, allow manual tapping
                onCellClick = onDaubCell
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1-100 Matrix Grid
        Matrix100Grid(
            calledNumbers = calledNumbers,
            latestCalledBall = latestCalledBall
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Exit Round Button
        Button(
            onClick = onExitGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("exit_game_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Text(
                text = "← EXIT ROUND",
                color = TextSecondary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
