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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.example.model.Room
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LobbyScreen(
    rooms: List<Room>,
    joinedRoomIds: Set<String> = emptySet(),
    isDailyClaimed: Boolean,
    onClaimReward: () -> Unit,
    onSelectRoom: (Room) -> Unit,
    onLeaveRoom: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val joinedRooms = rooms.filter { it.id in joinedRoomIds }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("lobby_screen")
    ) {
        // Active Joined Rooms Summary Banner
        if (joinedRooms.isNotEmpty()) {
            joinedRooms.forEach { joinedRoom ->
                val mins = joinedRoom.countdownSeconds / 60
                val secs = joinedRoom.countdownSeconds % 60
                val timerText = String.format("%02d:%02d", mins, secs)
                val readyToStart = joinedRoom.currentPlayers >= 2

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x2620C997))
                        .border(1.5.dp, MintPrimary, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .testTag("joined_status_banner_${joinedRoom.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MintPrimary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "✓ JOINED",
                                    color = BgDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = joinedRoom.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "⏳ $timerText",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👥 ${joinedRoom.currentPlayers} Joined Players",
                            color = MintLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("joined_players_count_${joinedRoom.id}")
                        )
                        Text(
                            text = if (readyToStart) "✓ 2+ Ready • Auto-Starts!" else "Need 2+ Players",
                            color = if (readyToStart) MintLight else GoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (readyToStart) {
                            "Game will start automatically when the 3-minute countdown reaches 00:00."
                        } else {
                            "Waiting for at least 2 players to auto-start when the 3-minute countdown finishes."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectRoom(joinedRoom) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("view_joined_ticket_${joinedRoom.id}"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MintLight)
                        ) {
                            Text(
                                text = "View Ticket",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { onLeaveRoom(joinedRoom.id) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("leave_joined_room_${joinedRoom.id}"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeDynamic),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A65))
                        ) {
                            Text(
                                text = "Leave & Refund",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Free Draw Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xE60F2D36), Color(0xE6133844))
                    )
                )
                .border(1.dp, Color(0x6620C997), RoundedCornerShape(16.dp))
                .padding(14.dp)
                .testTag("free_draw_banner"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🎁",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Column {
                    Text(
                        text = "FREE TIER DRAW ACTIVE",
                        color = MintLight,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isDailyClaimed) "Claimed for today! Come back tomorrow." else "Claim bonus ticket • Win up to 500 ETB!",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onClaimReward,
                enabled = !isDailyClaimed,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeDynamic,
                    disabledContainerColor = Color(0x66FF5722)
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("claim_daily_button")
            ) {
                Text(
                    text = if (isDailyClaimed) "Claimed" else "Claim",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section Title: Live Betting Rooms
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE BETTING ROOMS",
                color = GoldPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            val totalLivePlayers = rooms.sumOf { it.currentPlayers }
            Text(
                text = "● $totalLivePlayers Joined Players",
                color = MintPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("total_joined_players_label")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Room Cards
        rooms.forEach { room ->
            RoomCard(
                room = room,
                isJoined = room.id in joinedRoomIds,
                onClick = { onSelectRoom(room) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RoomCard(
    room: Room,
    isJoined: Boolean,
    onClick: () -> Unit
) {
    val mins = room.countdownSeconds / 60
    val secs = room.countdownSeconds % 60
    val timerText = String.format("%02d:%02d", mins, secs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCardElevated)
            .border(
                width = if (isJoined) 2.dp else 1.dp,
                color = if (isJoined) MintPrimary else BorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("room_card_${room.id}")
    ) {
        // Room header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(room.badgeColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = room.stakeEtb.toString(),
                        color = BgDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = room.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        if (isJoined) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MintPrimary)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "JOINED",
                                    color = BgDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = "Stake: ${room.stakeEtb} ETB / Ticket",
                        color = GoldLight,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BgSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⏳ $timerText",
                    color = Color(room.badgeColorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Room stats row with Joined Player count & 80% Prize / 20% House
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgDark)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 ${room.currentPlayers} Joined (${room.currentPlayers}/${room.maxPlayers})",
                    color = if (room.currentPlayers >= 2) MintLight else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = "🏆 Prize (80%): ${room.prizeEtb} ETB",
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (room.currentPlayers >= 2) "✓ 2+ Joined • Auto-starts at 00:00" else "Need 2+ Players • Pool: ${room.totalPoolEtb} ETB",
                    color = if (room.currentPlayers >= 2) MintPrimary else TextTertiary,
                    fontSize = 10.sp
                )
                Text(
                    text = "🏛️ House (20%): ${room.houseCommissionEtb} ETB",
                    color = MintLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Join room / Joined CTA button
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("join_room_button_${room.id}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isJoined) MintPrimary else OrangeDynamic
            )
        ) {
            Text(
                text = if (isJoined) "✓ JOINED • AUTO-START AT $timerText" else "SELECT TICKET & JOIN (${room.stakeEtb} ETB)",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                color = if (isJoined) BgDark else Color.White
            )
        }
    }
}
