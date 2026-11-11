package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PaymentMethod
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TelebirrBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.WinCelebrationData

@Composable
fun WalletPaymentModal(
    isDeposit: Boolean,
    currentBalanceEtb: Int,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, method: PaymentMethod, phoneNumber: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TELEBIRR) }
    var amountInput by remember { mutableStateOf(if (isDeposit) "100" else "50") }
    var phoneInput by remember { mutableStateOf("0912345678") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BgCardElevated)
                .border(1.5.dp, GoldPrimary, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag(if (isDeposit) "deposit_modal" else "withdraw_modal")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDeposit) "Top Up Balance" else "Instant Cashout",
                    color = GoldPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Text(
                text = "Select Payment Gateway (Ethiopia)",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Method Selector: Telebirr & CBE Birr
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodButton(
                    title = "Telebirr",
                    isSelected = selectedMethod == PaymentMethod.TELEBIRR,
                    accentColor = MintLight,
                    onClick = { selectedMethod = PaymentMethod.TELEBIRR },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    title = "CBE Birr",
                    isSelected = selectedMethod == PaymentMethod.CBE_BIRR,
                    accentColor = TelebirrBlue,
                    onClick = { selectedMethod = PaymentMethod.CBE_BIRR },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick chip amounts (deposit only)
            if (isDeposit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("50", "100", "200", "500").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (amountInput == preset) GoldPrimary else BgSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { amountInput = preset },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$preset",
                                color = if (amountInput == preset) BgDark else TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Text(
                    text = "Available Balance: $currentBalanceEtb ETB",
                    color = MintLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Amount input field
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { char -> char.isDigit() } },
                label = { Text(if (isDeposit) "Deposit Amount (ETB)" else "Cashout Amount (ETB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextSecondary,
                    focusedContainerColor = BgDark,
                    unfocusedContainerColor = BgDark
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone input field
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Account Phone Number (+251)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextSecondary,
                    focusedContainerColor = BgDark,
                    unfocusedContainerColor = BgDark
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = {
                    val amount = amountInput.toIntOrNull() ?: 0
                    if (amount > 0) {
                        onConfirm(amount, selectedMethod, phoneInput)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("modal_action_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
            ) {
                Text(
                    text = if (isDeposit) "PROCEED PAYMENT" else "CONFIRM CASHOUT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodButton(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) BgSurface else BgDark)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else BorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) accentColor else TextSecondary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun WinCelebrationModal(
    winData: WinCelebrationData,
    onContinue: () -> Unit
) {
    Dialog(onDismissRequest = onContinue) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BgCardElevated)
                .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp))
                .padding(24.dp)
                .testTag("win_modal"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👑",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "BINGO WINNER!",
                fontFamily = FontFamily.Serif,
                color = GoldPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                text = winData.reason,
                color = MintLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Text(
                text = "+${winData.payoutEtb} ETB",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Credited to your Ethiopian wallet balance",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("win_modal_continue_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
            ) {
                Text(
                    text = "CONTINUE PLAYING",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}
