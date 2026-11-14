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
    defaultPhone: String = "",
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, method: PaymentMethod, phoneNumber: String) -> Unit
) {
    SecureWalletModal(
        initialIsDeposit = isDeposit,
        currentBalanceEtb = currentBalanceEtb,
        defaultPhone = defaultPhone,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
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
