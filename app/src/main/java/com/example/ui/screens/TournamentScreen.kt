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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.LeaderboardEntry
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TournamentTab

@Composable
fun TournamentScreen(
    currentTab: TournamentTab,
    leaderboards: Map<TournamentTab, List<LeaderboardEntry>>,
    tournamentSecondsLeft: Int,
    onTabSelected: (TournamentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = leaderboards[currentTab] ?: emptyList()
    val secondsStr = String.format("%02ds", tournamentSecondsLeft)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("tournament_screen")
    ) {
        // Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TournamentTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BgSurface else BgCard)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GoldPrimary else BorderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("tournament_tab_${tab.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = if (isSelected) GoldPrimary else TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grand Prize Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(BgCardElevated, BgCard)
                    )
                )
                .border(1.dp, GoldPrimary, RoundedCornerShape(18.dp))
                .padding(14.dp)
                .testTag("tournament_grand_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GRAND PRIZE POOL",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "100,000 ETB",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }

                // Timer Boxes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TimerBox(text = "03d")
                    Text(":", color = TextSecondary, fontWeight = FontWeight.Bold)
                    TimerBox(text = "14h")
                    Text(":", color = TextSecondary, fontWeight = FontWeight.Bold)
                    TimerBox(text = "22m")
                    Text(":", color = TextSecondary, fontWeight = FontWeight.Bold)
                    TimerBox(text = secondsStr)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOP RANKINGS",
                color = GoldPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "Live Verified",
                color = MintPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ranked Cards or Empty State
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆 No tournament records yet.\nWin a live multiplayer bingo game to claim the #1 spot!",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            entries.forEach { entry ->
                RankCard(entry = entry)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TimerBox(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BgSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RankCard(entry: LeaderboardEntry) {
    val borderColor = when (entry.rank) {
        1 -> GoldPrimary
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFFCD7F32)
        else -> BorderColor
    }

    val iconOrRank = when (entry.rank) {
        1 -> "👑"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCardElevated)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("rank_card_${entry.rank}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconOrRank,
                    fontSize = if (entry.rank <= 3) 20.sp else 12.sp,
                    color = if (entry.rank > 3) TextSecondary else TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = entry.name,
                    color = if (entry.isCurrentUser) GoldPrimary else TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${entry.phoneMasked} • ${entry.wins} Wins",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.prizeEtb} ETB",
                color = when (entry.rank) {
                    1 -> GoldPrimary
                    2 -> Color(0xFFE2E8F0)
                    3 -> Color(0xFFCD7F32)
                    else -> MintLight
                },
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = "Rank #${entry.rank}",
                color = if (entry.rank == 1) MintPrimary else TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
