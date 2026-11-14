package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PaymentMethod
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CbeBirrMaroon
import com.example.ui.theme.CbeBirrOrange
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
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

const val DEPOSIT_ACCOUNT_HOLDER = "Yemane"
const val DEPOSIT_ACCOUNT_NUMBER = "0939383425"

enum class WalletStep {
    INPUT,
    TRANSFER_CONFIRM,
    PROCESSING,
    RECEIPT
}

@Composable
fun SecureWalletModal(
    initialIsDeposit: Boolean,
    currentBalanceEtb: Int,
    defaultPhone: String = "",
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, method: PaymentMethod, phoneNumber: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDeposit by remember { mutableStateOf(initialIsDeposit) }
    var currentStep by remember { mutableStateOf(WalletStep.INPUT) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TELEBIRR) }
    var amountInput by remember { mutableStateOf(if (initialIsDeposit) "100" else "50") }
    var phoneInput by remember(defaultPhone) {
        mutableStateOf(if (defaultPhone.any { it.isDigit() }) defaultPhone else "")
    }
    var transferRefInput by remember { mutableStateOf("") }

    var generatedReference by remember { mutableStateOf("") }
    var transactionTimestamp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val copyDepositNumber: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Deposit Account Number", DEPOSIT_ACCOUNT_NUMBER)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            "Copied $DEPOSIT_ACCOUNT_HOLDER ($DEPOSIT_ACCOUNT_NUMBER)",
            Toast.LENGTH_SHORT
        ).show()
    }

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
                                text = "Yem Ethiopian Wallet",
                                fontFamily = OutfitFontFamily,
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Telebirr & CBE Birr • Instant Balance",
                                fontFamily = PlusJakartaFontFamily,
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
                                    .heightIn(min = 48.dp)
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
                                    text = "Deposit Funds",
                                    fontFamily = OutfitFontFamily,
                                    color = if (isDeposit) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
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
                                    text = "Instant Cashout",
                                    fontFamily = OutfitFontFamily,
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
                                fontFamily = PlusJakartaFontFamily,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$currentBalanceEtb ETB",
                                fontFamily = OutfitFontFamily,
                                color = GoldLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Gateway Selection Section
                        Text(
                            text = "SELECT PAYMENT GATEWAY",
                            fontFamily = PlusJakartaFontFamily,
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
                            GatewaySelectionCard(
                                title = "Telebirr",
                                subtitle = if (isDeposit) "To: $DEPOSIT_ACCOUNT_HOLDER ($DEPOSIT_ACCOUNT_NUMBER)" else "USSD (*127#) & SuperApp",
                                badgeText = "0% FEE • INSTANT",
                                isSelected = selectedMethod == PaymentMethod.TELEBIRR,
                                brandColor = TelebirrBlue,
                                secondaryColor = TelebirrDarkBlue,
                                onClick = { selectedMethod = PaymentMethod.TELEBIRR },
                                modifier = Modifier.weight(1f)
                            )

                            GatewaySelectionCard(
                                title = "CBE Birr",
                                subtitle = if (isDeposit) "To: $DEPOSIT_ACCOUNT_HOLDER ($DEPOSIT_ACCOUNT_NUMBER)" else "Commercial Bank of Ethiopia",
                                badgeText = if (isDeposit) "0% FEE • INSTANT" else "1% FEE CASHOUT",
                                isSelected = selectedMethod == PaymentMethod.CBE_BIRR,
                                brandColor = CbeBirrOrange,
                                secondaryColor = CbeBirrMaroon,
                                onClick = { selectedMethod = PaymentMethod.CBE_BIRR },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Recipient Deposit Account Card (Always visible in Deposit mode for both Telebirr & CBE Birr)
                        if (isDeposit) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val accentColor = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else CbeBirrOrange
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BgDark)
                                    .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .testTag("deposit_recipient_card")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Deposit Account",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEPOSIT TO (${selectedMethod.displayName.uppercase()})",
                                        fontFamily = OutfitFontFamily,
                                        color = accentColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.4.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Account Holder",
                                            fontFamily = PlusJakartaFontFamily,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = DEPOSIT_ACCOUNT_HOLDER,
                                            fontFamily = OutfitFontFamily,
                                            color = GoldLight,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            modifier = Modifier.testTag("deposit_account_holder")
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${selectedMethod.displayName} Account Number",
                                            fontFamily = PlusJakartaFontFamily,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = DEPOSIT_ACCOUNT_NUMBER,
                                            fontFamily = OutfitFontFamily,
                                            color = MintLight,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            letterSpacing = 0.8.sp,
                                            modifier = Modifier.testTag("deposit_account_number")
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = copyDepositNumber,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("copy_deposit_account_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Account Number",
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Copy",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Amount Chips
                        Text(
                            text = if (isDeposit) "QUICK DEPOSIT AMOUNTS" else "QUICK CASHOUT AMOUNTS",
                            fontFamily = PlusJakartaFontFamily,
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

                        // Player's Account Phone Field
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                phoneInput = it
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    when {
                                        isDeposit && selectedMethod == PaymentMethod.TELEBIRR ->
                                            "Your Telebirr Sender Number (+251 / 09...)"
                                        isDeposit ->
                                            "Your CBE Birr Sender Number (+251 / 09...)"
                                        selectedMethod == PaymentMethod.TELEBIRR ->
                                            "Your Telebirr Cashout Number (+251 / 09...)"
                                        else ->
                                            "Your CBE Birr Cashout Account (+251 / 09...)"
                                    }
                                )
                            },
                            placeholder = {
                                Text("e.g., 09... or +251 9...", color = TextTertiary, fontSize = 12.sp)
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
                            if (isDeposit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Deposit Recipient:", color = TextSecondary, fontSize = 11.sp)
                                    Text("$DEPOSIT_ACCOUNT_HOLDER ($DEPOSIT_ACCOUNT_NUMBER)", color = MintLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
                                if (phoneInput.filter { it.isDigit() }.length < 9) {
                                    errorMessage = "Please enter your valid 9 or 10-digit Ethiopian mobile number"
                                    return@Button
                                }

                                transferRefInput = ""
                                errorMessage = null
                                currentStep = WalletStep.TRANSFER_CONFIRM
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("wallet_proceed_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else CbeBirrOrange
                            )
                        ) {
                            Text(
                                text = if (isDeposit)
                                    "CONTINUE ${selectedMethod.displayName.uppercase()} DEPOSIT"
                                else
                                    "CONFIRM CASHOUT VIA ${selectedMethod.displayName.uppercase()}",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    WalletStep.TRANSFER_CONFIRM -> {
                        val amount = amountInput.toIntOrNull() ?: 0
                        val accentColor = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else CbeBirrOrange

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
                                text = if (isDeposit)
                                    "${selectedMethod.displayName} Deposit Confirmation"
                                else
                                    "${selectedMethod.displayName} Cashout Confirmation",
                                fontFamily = OutfitFontFamily,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selectedMethod == PaymentMethod.TELEBIRR) Color(0xFF072B3A) else Color(0xFF2A0F1A)
                                )
                                .border(1.5.dp, accentColor, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedMethod == PaymentMethod.TELEBIRR)
                                        "ethio telecom telebirr"
                                    else
                                        "Commercial Bank of Ethiopia",
                                    color = accentColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = selectedMethod.displayName.uppercase(),
                                        color = GoldLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isDeposit) {
                                Text(
                                    text = "Send $amount ETB via ${selectedMethod.displayName} to:",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ReceiptRow("Account Holder:", DEPOSIT_ACCOUNT_HOLDER, highlightColor = GoldLight)
                                ReceiptRow("Account Number:", DEPOSIT_ACCOUNT_NUMBER, highlightColor = MintLight)
                                ReceiptRow("Deposit Amount:", "$amount ETB", highlightColor = Color.White)
                                ReceiptRow("Your Sender Phone:", phoneInput)

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = copyDepositNumber,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MintLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MintLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Account",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Copy $DEPOSIT_ACCOUNT_NUMBER ($DEPOSIT_ACCOUNT_HOLDER)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "${selectedMethod.displayName} Transaction Reference (Optional):",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = transferRefInput,
                                    onValueChange = { transferRefInput = it },
                                    placeholder = { Text("Paste SMS Ref / FT Number", color = TextTertiary, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "Transaction Reference",
                                            tint = accentColor
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = BorderColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = BgDark,
                                        unfocusedContainerColor = BgDark
                                    ),
                                    singleLine = true
                                )
                            } else {
                                ReceiptRow("Gateway:", selectedMethod.displayName, highlightColor = accentColor)
                                ReceiptRow("Destination Phone:", phoneInput, highlightColor = MintLight)
                                ReceiptRow("Cashout Amount:", "$amount ETB", highlightColor = GoldLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                currentStep = WalletStep.PROCESSING
                                scope.launch {
                                    delay(900)
                                    val prefix = if (selectedMethod == PaymentMethod.TELEBIRR) "TLB" else "CBE"
                                    val manualRef = transferRefInput.trim()
                                    generatedReference = if (manualRef.isNotBlank()) {
                                        manualRef
                                    } else {
                                        "$prefix-ET-${Random.nextInt(100000, 999999)}"
                                    }
                                    transactionTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

                                    onConfirm(amount, selectedMethod, phoneInput.trim())
                                    currentStep = WalletStep.RECEIPT
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("authorize_payment_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMethod == PaymentMethod.TELEBIRR) TelebirrBlue else OrangeDynamic
                            )
                        ) {
                            Text(
                                text = if (isDeposit)
                                    "CONFIRM ${selectedMethod.displayName.uppercase()} DEPOSIT"
                                else
                                    "CONFIRM ${selectedMethod.displayName.uppercase()} CASHOUT",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    WalletStep.PROCESSING -> {
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
                                    "Recording Telebirr Transaction..."
                                else
                                    "Recording CBE Birr Transaction...",
                                fontFamily = OutfitFontFamily,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isDeposit)
                                    "Recipient: $DEPOSIT_ACCOUNT_HOLDER ($DEPOSIT_ACCOUNT_NUMBER)"
                                else
                                    "Destination: $phoneInput",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WalletStep.RECEIPT -> {
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
                                text = if (isDeposit) "DEPOSIT RECORDED!" else "CASHOUT INITIATED!",
                                fontFamily = OutfitFontFamily,
                                color = MintLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = if (isDeposit)
                                    "Credited to your Yem Bingo wallet."
                                else
                                    "Birr sent to your ${selectedMethod.displayName} mobile wallet.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

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
                                if (isDeposit) {
                                    ReceiptRow("Deposited To:", "$DEPOSIT_ACCOUNT_HOLDER ($DEPOSIT_ACCOUNT_NUMBER)", highlightColor = GoldLight)
                                    ReceiptRow("Sender Phone:", phoneInput)
                                } else {
                                    ReceiptRow("Account:", phoneInput)
                                }
                                ReceiptRow("Amount:", "${if (isDeposit) "+" else "-"}$amount ETB", highlightColor = if (isDeposit) MintLight else GoldPrimary)
                                ReceiptRow("Status:", "COMPLETED", highlightColor = MintPrimary)
                                ReceiptRow("Date & Time:", transactionTimestamp)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

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
                                    .heightIn(min = 48.dp)
                                    .testTag("receipt_done_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
                            ) {
                                Text(
                                    text = "DONE / BACK TO GAME",
                                    fontFamily = OutfitFontFamily,
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
            fontSize = 10.sp,
            lineHeight = 13.sp,
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
            .height(36.dp)
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
