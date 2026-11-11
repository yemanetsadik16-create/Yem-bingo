package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionItem
import com.example.model.UserProfile
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    profile: UserProfile,
    transactions: List<TransactionItem>,
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("profile_screen")
    ) {
        // User Profile & Balance Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCardElevated)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            // Avatar & Identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(GoldPrimary, OrangeDynamic))
                        )
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🦁",
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VIP ${profile.vipLevel}",
                                color = BgDark,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = "${profile.phoneMasked} • ${profile.location}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Text(
                        text = "Verified Player",
                        fontSize = 11.sp,
                        color = MintPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Balance & Deposit/Cashout Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgDark)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT BALANCE",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profile.balanceEtb} ETB",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDepositClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("profile_deposit_button")
                    ) {
                        Text(
                            text = "Deposit",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onWithdrawClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MintPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintPrimary),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("profile_cashout_button")
                    ) {
                        Text(
                            text = "Cashout",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Games Played",
                value = profile.gamesPlayed.toString(),
                valueColor = GoldPrimary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Winnings",
                value = "${profile.totalWinningsEtb} ETB",
                valueColor = MintLight,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Win Rate",
                value = "${profile.winRatePercent}%",
                valueColor = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Daily Streak",
                value = "${profile.dailyStreakDays} Days 🔥",
                valueColor = OrangeDynamic,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Referral & Invite Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCardElevated)
                .border(1.dp, Color(0x6620C997), RoundedCornerShape(16.dp))
                .padding(14.dp)
                .testTag("referral_card")
        ) {
            Text(
                text = "REFER FRIENDS & GET 50 ETB",
                fontWeight = FontWeight.ExtraBold,
                color = MintLight,
                fontSize = 13.sp
            )
            Text(
                text = "Share your promo code with friends to receive bonus ETB instantly.",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgDark)
                    .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.promoCode,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary,
                    letterSpacing = 1.sp,
                    fontSize = 13.sp
                )

                Text(
                    text = "COPY",
                    fontSize = 11.sp,
                    color = OrangeDynamic,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Yem Bingo Promo Code", profile.promoCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Promo code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(4.dp)
                        .testTag("copy_promo_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sound & Vibration Settings Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCardElevated)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "GAME SETTINGS",
                color = GoldPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Effects", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = onToggleSound,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldPrimary,
                        checkedTrackColor = BgSurface,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = BgDark
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration / Haptic", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = isVibrationEnabled,
                    onCheckedChange = onToggleVibration,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MintPrimary,
                        checkedTrackColor = BgSurface,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = BgDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transactions History Card
        if (transactions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                .background(BgCardElevated)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
            ) {
                Text(
                    text = "RECENT TRANSACTIONS",
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                transactions.take(5).forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tx.title,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tx.method} • ${tx.timestampFormatted}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "${if (tx.isCredit) "+" else "-"}${tx.amountEtb} ETB",
                            color = if (tx.isCredit) MintLight else OrangeDynamic,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Log Out / Switch Account Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("profile_logout_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Text(
                text = "🚪 LOG OUT / SWITCH ACCOUNT",
                color = OrangeDynamic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCardElevated)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
    }
}
