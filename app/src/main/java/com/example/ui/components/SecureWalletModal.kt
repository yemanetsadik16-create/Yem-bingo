package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PaymentMethod
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CbeBirrGold
import com.example.ui.theme.CbeBirrMaroon
import com.example.ui.theme.CbeBirrOrange
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TelebirrBlue
import com.example.ui.theme.TelebirrDarkBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class WalletStep {
    INPUT,
    MOCK_GATEWAY,
    PROCESSING,
    RECEIPT
}

@Composable
fun SecureWalletModal(
    initialIsDeposit: Boolean,
    currentBalanceEtb: Int,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, method: PaymentMethod, phoneNumber: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDeposit by remember { mutableStateOf(initialIsDeposit) }
    var currentStep by remember { mutableStateOf(WalletStep.INPUT) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TELEBIRR) }
    var amountInput by remember { mutableStateOf(if (initialIsDeposit) "100" else "50") }
    var phoneInput by remember { mutableStateOf("0912345678") }

    // Mock Gateway State
    var telebirrPin by remember { mutableStateOf("") }
    var cbeOtp by remember { mutableStateOf("") }
    var generatedReference by remember { mutableStateOf("") }
    var transactionTimestamp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (currentStep != WalletStep.PROCESSING) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .background(BgCardElevated)
                .border(1.5.dp, GoldPrimary, RoundedCornerShape(22.dp))
                .padding(18.dp)
                .testTag("secure_wallet_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Shield",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Yem Secure Ethiopian Wallet",
                                color = GoldPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "🔒 256-Bit SSL Encrypted • Telebirr & CBE",
                                color = MintLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (currentStep != WalletStep.PROCESSING) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Wallet",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (currentStep) {
                    WalletStep.INPUT -> {
                        // Deposit / Cashout Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgDark)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDeposit) OrangeDynamic else Color.Transparent)
                                    .clickable {
                                        isDeposit = true
                                        errorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↓ Deposit Funds",
                                    color = if (isDeposit) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isDeposit) MintPrimary else Color.Transparent)
                                    .clickable {
                                        isDeposit = false
                                        errorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↑ Instant Cashout",
                                    color = if (!isDeposit) BgDark else TextSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current Balance Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Balance:",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$currentBalanceEtb ETB",
                                color = GoldLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Gateway Selection Section
                        Text(
                            text = "SELECT PAYMENT GATEWAY",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Telebirr Card
                            GatewaySelectionCard(
                                title = "Telebirr",
                                subtitle = "USSD (*127#) & SuperApp",
                                badgeText = "0% FEE • INSTANT",
                                isSelected = selectedMethod == PaymentMethod.TELEBIRR,
                                brandColor = TelebirrBlue,
                                secondaryColor = TelebirrDarkBlue,
                                onClick = { selectedMethod = PaymentMethod.TELEBIRR },
                                modifier = Modifier.weight(1f)
                            )

                            // CBE Birr Card
                            GatewaySelectionCard(
                                title = "CBE Birr",
                                subtitle = "Commercial Bank of Ethiopia",
                                badgeText = if (isDeposit) "0% FEE • INSTANT" else "1% FEE CASHOUT",
                                isSelected = selectedMethod == PaymentMethod.CBE_BIRR,
                                brandColor = CbeBirrOrange,
                                secondaryColor = CbeBirrMaroon,
                                onClick = { selectedMethod = PaymentMethod.CBE_BIRR },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Amount Chips
                        Text(
                            text = if (isDeposit) "QUICK DEPOSIT AMOUNTS" else "QUICK CASHOUT AMOUNTS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val presets = if (isDeposit) {
                            listOf("50", "100", "200", "500", "1000", "2500")
                        } else {
                            listOf("50", "100", "250", "500", "1000", "2000")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.take(3).forEach { p ->
                                QuickChip(
                                    amount = p,
                                    isSelected = amountInput == p,
                                    onClick = {
                                        amountInput = p
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.takeLast(3).forEach { p ->
                                QuickChip(
                                    amount = p,
                                    isSelected = amountInput == p,
                                    onClick = {
                                        amountInput = p
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Amount Field
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = {
                                amountInput = it.filter { ch -> ch.isDigit() }
                                errorMessage = null
                            },
                            label = { Text(if (isDeposit) "Amount to Deposit (ETB)" else "Amount to Cashout (ETB)") },
                            suffix = {
                                Text(
                                    text = "ETB",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            },
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

                        // Account Phone Field
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                phoneInput = it
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    if (selectedMethod == PaymentMethod.TELEBIRR)
                                        "Telebirr Mobile Number (+251)"
                                    else
                                        "CBE Birr Account / Phone (+251)"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Phone",
                                    tint = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else CbeBirrOrange
                                )
                            },
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Calculation Breakdown Card
                        val parsedAmount = amountInput.toIntOrNull() ?: 0
                        val fee = if (!isDeposit && selectedMethod == PaymentMethod.CBE_BIRR) {
                            (parsedAmount * 1) / 100
                        } else 0
                        val netAmount = if (isDeposit) parsedAmount else parsedAmount - fee

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transaction Subtotal:", color = TextSecondary, fontSize = 11.sp)
                                Text("$parsedAmount ETB", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gateway Service Fee:", color = TextSecondary, fontSize = 11.sp)
                                Text(if (fee == 0) "0 ETB (FREE)" else "$fee ETB (1%)", color = MintLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isDeposit) "Total Credited to Wallet:" else "Total Transferred to Mobile:",
                                    color = GoldPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "$netAmount ETB",
                                    color = GoldPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = OrangeDynamic,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Proceed Button
                        Button(
                            onClick = {
                                val amount = amountInput.toIntOrNull() ?: 0
                                if (amount <= 0) {
                                    errorMessage = "Please enter a valid amount greater than 0 ETB"
                                    return@Button
                                }
                                if (!isDeposit && amount > currentBalanceEtb) {
                                    errorMessage = "Insufficient balance! Your available balance is $currentBalanceEtb ETB"
                                    return@Button
                                }
                                if (phoneInput.trim().length < 9) {
                                    errorMessage = "Please enter a valid 9 or 10-digit Ethiopian mobile number"
                                    return@Button
                                }

                                // Proceed to Mock Gateway Step
                                telebirrPin = ""
                                cbeOtp = ""
                                currentStep = WalletStep.MOCK_GATEWAY
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("wallet_proceed_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else CbeBirrOrange
                            )
                        ) {
                            Text(
                                text = if (isDeposit)
                                    "PROCEED VIA ${selectedMethod.displayName.uppercase()}"
                                else
                                    "CONFIRM CASHOUT VIA ${selectedMethod.displayName.uppercase()}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    WalletStep.MOCK_GATEWAY -> {
                        // Interactive Mock Gateway Authorization Screen
                        val amount = amountInput.toIntOrNull() ?: 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentStep = WalletStep.INPUT }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextSecondary
                                )
                            }
                            Text(
                                text = "${selectedMethod.displayName} Security Verification",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedMethod == PaymentMethod.TELEBIRR) {
                            // Telebirr USSD / App Push Simulation
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF072B3A))
                                    .border(1.5.dp, TelebirrBlue, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ethio telecom telebirr",
                                        color = TelebirrBlue,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x330091DA))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "USSD PUSH ACTIVE",
                                            color = MintLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (isDeposit)
                                        "Payment Request from: YEM BINGO ETHIOPIA\nMerchant ID: YEM-ET-9941\nAmount: $amount ETB\nAccount: $phoneInput"
                                    else
                                        "Cashout Transfer to: $phoneInput\nOrigin: YEM BINGO WALLET\nAmount: $amount ETB",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Enter 4-Digit Telebirr PIN to Authorize:",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = telebirrPin,
                                    onValueChange = { if (it.length <= 4) telebirrPin = it.filter { c -> c.isDigit() } },
                                    placeholder = { Text("••••") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TelebirrBlue,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = BgDark,
                                        unfocusedContainerColor = BgDark
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { telebirrPin = "7788" },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MintLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MintLight)
                                ) {
                                    Text("⚡ Auto-Fill Mock PIN (7788)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // CBE Birr SMS OTP Simulation
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2A0F1A))
                                    .border(1.5.dp, CbeBirrOrange, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Commercial Bank of Ethiopia",
                                        color = CbeBirrOrange,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x33FF6D00))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CBE BIRR",
                                            color = GoldLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (isDeposit)
                                        "CBE Core Banking Verification\nBeneficiary: Yem Bingo Ethiopian Entertainment\nAmount: $amount ETB\nTarget Account: $phoneInput"
                                    else
                                        "CBE Cashout Verification\nBeneficiary Account: $phoneInput\nAmount: $amount ETB",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Enter 6-Digit SMS OTP Code:",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = cbeOtp,
                                    onValueChange = { if (it.length <= 6) cbeOtp = it.filter { c -> c.isDigit() } },
                                    placeholder = { Text("6-Digit Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CbeBirrOrange,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = BgDark,
                                        unfocusedContainerColor = BgDark
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { cbeOtp = "849201" },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CbeBirrGold),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CbeBirrGold)
                                ) {
                                    Text("🔑 Auto-Fill Demo OTP (849201)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                currentStep = WalletStep.PROCESSING
                                scope.launch {
                                    delay(1600) // Realistic network handshake delay
                                    val prefix = if (selectedMethod == PaymentMethod.TELEBIRR) "TLB" else "CBE"
                                    generatedReference = "$prefix-ET-2026-${Random.nextInt(100000, 999999)}"
                                    transactionTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

                                    // Trigger real deposit/withdraw in ViewModel & Firestore
                                    onConfirm(amount, selectedMethod, phoneInput)

                                    currentStep = WalletStep.RECEIPT
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("authorize_payment_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else OrangeDynamic
                            )
                        ) {
                            Text(
                                text = if (selectedMethod == PaymentMethod.TELEBIRR)
                                    "AUTHORIZE TELEBIRR USSD PAYMENT"
                                else
                                    "AUTHORIZE CBE BIRR TRANSFER",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    WalletStep.PROCESSING -> {
                        // Simulated Bank / Gateway Processing
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else OrangeDynamic,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(52.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (selectedMethod == PaymentMethod.TELEBIRR)
                                    "Contacting Telebirr Mobile Money..."
                                else
                                    "Connecting to CBE Core Banking...",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Encrypting transaction credentials with 256-Bit SSL...",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WalletStep.RECEIPT -> {
                        // Transaction Success Receipt
                        val amount = amountInput.toIntOrNull() ?: 0

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3320C997))
                                    .border(2.dp, MintPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = MintLight,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isDeposit) "DEPOSIT SUCCESSFUL!" else "CASHOUT INITIATED!",
                                color = MintLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = if (isDeposit)
                                    "Funds credited instantly to your Yem Bingo balance."
                                else
                                    "Birr sent to your ${selectedMethod.displayName} mobile wallet.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Official Ethiopian Receipt Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BgDark)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                ReceiptRow("Transaction ID:", generatedReference)
                                ReceiptRow("Gateway:", selectedMethod.displayName)
                                ReceiptRow("Amount:", "${if (isDeposit) "+" else "-"}$amount ETB", highlightColor = if (isDeposit) MintLight else GoldPrimary)
                                ReceiptRow("Account:", phoneInput)
                                ReceiptRow("Status:", "COMPLETED (VERIFIED)", highlightColor = MintPrimary)
                                ReceiptRow("Date & Time:", transactionTimestamp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Copy Reference Button
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Transaction Reference", generatedReference)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied: $generatedReference", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COPY TRANSACTION REFERENCE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("receipt_done_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
                            ) {
                                Text(
                                    text = "DONE / BACK TO GAME",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GatewaySelectionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    isSelected: Boolean,
    brandColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected)
                    Modifier.background(Brush.verticalGradient(listOf(BgSurface, secondaryColor.copy(alpha = 0.35f))))
                else
                    Modifier.background(BgDark)
            )
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) brandColor else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (isSelected) brandColor else TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = brandColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) brandColor.copy(alpha = 0.2f) else BgSurface)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                color = if (isSelected) brandColor else TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun QuickChip(
    amount: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) GoldPrimary else BgSurface)
            .border(1.dp, if (isSelected) GoldPrimary else BorderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$amount",
            color = if (isSelected) BgDark else TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    highlightColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = highlightColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
