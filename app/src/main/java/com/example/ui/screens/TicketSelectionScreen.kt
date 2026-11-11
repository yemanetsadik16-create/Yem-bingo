package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.example.model.Room
import com.example.ui.components.BingoCardView
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TextSecondary

@Composable
fun TicketSelectionScreen(
    room: Room,
    selectedTicketNumber: Int,
    card: BingoCard?,
    onBackToLobby: () -> Unit,
    onSelectTicket: (Int) -> Unit,
    onRefreshCard: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("ticket_selection_screen")
    ) {
        // Top selector card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCardElevated)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Lobby",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onBackToLobby() }
                        .padding(4.dp)
                        .testTag("back_to_lobby_button")
                )

                Text(
                    text = "${room.name} (${room.stakeEtb} ETB)",
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )

                Text(
                    text = "Selected #$selectedTicketNumber",
                    color = MintPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Winner (80%): ${room.prizeEtb} ETB",
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "🏛️ House (20%): ${room.houseCommissionEtb} ETB",
                    color = MintLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Minute Auto-Start Countdown Banner
            val mins = room.countdownSeconds / 60
            val secs = room.countdownSeconds % 60
            val timerText = String.format("%02d:%02d", mins, secs)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2620C997))
                    .border(1.dp, MintPrimary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏳ Auto-Start: $timerText",
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (room.currentPlayers >= 2) "✓ ${room.currentPlayers} Players (Min 2 Met)" else "⚠️ Waiting (Need 2+ Players)",
                    color = if (room.currentPlayers >= 2) MintLight else Color(0xFFFF8A65),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal scrolling ticket chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (70..95).forEach { ticketNum ->
                    val isSelected = ticketNum == selectedTicketNumber
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(listOf(GoldPrimary, GoldDark))
                                } else {
                                    Brush.verticalGradient(listOf(BgSurface, BgSurface))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectTicket(ticketNum) }
                            .testTag("ticket_chip_$ticketNum"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$ticketNum",
                            color = if (isSelected) BgDark else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5x5 Card Preview
        if (card != null) {
            BingoCardView(
                card = card,
                isInteractive = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Refresh & Start
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onRefreshCard,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("refresh_card_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = BgCard,
                    contentColor = GoldPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
            ) {
                Text(
                    text = "↻ Refresh",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .weight(2f)
                    .height(44.dp)
                    .testTag("start_game_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
            ) {
                Text(
                    text = "▶ START GAME (${room.stakeEtb} ETB)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}
