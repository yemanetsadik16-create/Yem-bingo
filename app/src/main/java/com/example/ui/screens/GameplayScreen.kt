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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BingoViewModel
import kotlin.math.roundToInt

@Composable
fun GameplayScreen(
    currentCard: BingoCard?,
    calledNumbers: List<Int>,
    latestCalledBall: Int?,
    amharicAnnouncement: String? = null,
    isAutoDaub: Boolean,
    isCallerMuted: Boolean = false,
    callerVolume: Float = 0.9f,
    nextBallCountdownSeconds: Int = 6,
    callerStatusText: String = "Amharic Caller Man (Iapetus)",
    amharicVoiceError: String? = null,
    onToggleCallerMute: () -> Unit = {},
    onCallerVolumeChange: (Float) -> Unit = {},
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
            // Caller Header Row: Avatar, Title, 6s Timer Pill, and Auto-Daub
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFC107)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎙️", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "የአማርኛ ጠሪ • Live Caller",
                            color = GoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = callerStatusText,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 6-Second Interval Countdown Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFC107))
                        .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("next_ball_timer_pill")
                ) {
                    Text(
                        text = "⏱ ${nextBallCountdownSeconds}s / 6s",
                        color = GoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

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
                        text = "Auto: ${if (isAutoDaub) "ON" else "OFF"}",
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
                            "STARTING ROUND • PRE-FETCHING VOICE...",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )

                    // Spoken Amharic Callout Text
                    Text(
                        text = amharicAnnouncement ?: "ጨዋታው ተጀምሯል! መልካም ዕድል!",
                        color = MintLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${calledNumbers.size}/75 Numbers Called • Strict 6s Interval",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Controls Bar: Mute/Unmute Toggle + Volume Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleCallerMute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("caller_mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isCallerMuted)
                            Icons.AutoMirrored.Filled.VolumeOff
                        else
                            Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isCallerMuted) "Unmute Amharic Caller" else "Mute Amharic Caller",
                        tint = if (isCallerMuted) OrangeDynamic else MintLight
                    )
                }

                Text(
                    text = if (isCallerMuted) "MUTED" else "VOL",
                    color = if (isCallerMuted) OrangeDynamic else TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    modifier = Modifier.width(38.dp)
                )

                Slider(
                    value = if (isCallerMuted) 0f else callerVolume,
                    onValueChange = onCallerVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("caller_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = MintPrimary,
                        inactiveTrackColor = BorderColor
                    )
                )

                Text(
                    text = "${((if (isCallerMuted) 0f else callerVolume) * 100).roundToInt()}%",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            if (!amharicVoiceError.isNullOrBlank()) {
                Text(
                    text = amharicVoiceError,
                    color = OrangeDynamic,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
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
