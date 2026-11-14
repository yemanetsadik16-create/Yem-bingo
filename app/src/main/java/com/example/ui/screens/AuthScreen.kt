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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    initialPhone: String = "",
    initialRememberMe: Boolean = true,
    isSigningIn: Boolean = false,
    authError: String? = null,
    onLoginSubmit: (phone: String, pin: String, rememberMe: Boolean) -> Unit = { _, _, _ -> },
    onCreateAccountSubmit: (fullName: String, phone: String, city: String, pin: String, promoCode: String) -> Unit = { _, _, _, _, _ -> },
    onResetPin: (phone: String, newPin: String) -> String? = { _, _ -> null },
    onGoogleSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLoginTab by remember { mutableStateOf(true) }

    // Log In tab state
    var loginPhone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var loginPin by remember { mutableStateOf("") }
    var loginPinVisible by remember { mutableStateOf(false) }
    var rememberMe by remember(initialRememberMe) { mutableStateOf(initialRememberMe) }

    // Create Account tab state
    var fullName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("Addis Ababa") }
    var regPin by remember { mutableStateOf("") }
    var regPinVisible by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }

    // Reset PIN dialog state
    var showResetPinDialog by remember { mutableStateOf(false) }
    var resetPhone by remember { mutableStateOf(initialPhone) }
    var resetNewPin by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }

    val ethiopianCities = listOf(
        "Addis Ababa",
        "Hawassa",
        "Bahir Dar",
        "Dire Dawa",
        "Adama",
        "Mekelle",
        "Gondar"
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GoldPrimary,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = GoldPrimary,
        unfocusedLabelColor = TextSecondary,
        focusedContainerColor = BgDark,
        unfocusedContainerColor = BgDark
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Brand Hero: Glowing Ball + Title
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(GoldLight, OrangeDynamic)
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "77",
                color = BgDark,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "YEM BINGO",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = GoldLight
        )

        Text(
            text = "ETHIOPIA'S #1 LIVE MULTIPLAYER",
            fontFamily = PlusJakartaFontFamily,
            color = MintPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        Text(
            text = "Play Live with Ethiopian Players across Addis & Regions",
            fontFamily = PlusJakartaFontFamily,
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // Segmented Toggle: LOG IN / CREATE ACCOUNT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgDark)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLoginTab) GoldPrimary else Color.Transparent)
                    .clickable { isLoginTab = true }
                    .testTag("tab_login"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOG IN",
                    fontFamily = OutfitFontFamily,
                    color = if (isLoginTab) BgDark else TextSecondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isLoginTab) GoldPrimary else Color.Transparent)
                    .clickable { isLoginTab = false }
                    .testTag("tab_create_account"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CREATE ACCOUNT",
                    fontFamily = OutfitFontFamily,
                    color = if (!isLoginTab) BgDark else TextSecondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Auth Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BgCardElevated)
                .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            if (isLoginTab) {
                // LOG IN TAB
                Text(
                    text = "Welcome Back!",
                    fontFamily = OutfitFontFamily,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Enter your registered Ethiopian mobile number and PIN",
                    fontFamily = PlusJakartaFontFamily,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = loginPhone,
                    onValueChange = { loginPhone = it },
                    label = { Text("Phone Number (+251 9... / 09...)") },
                    leadingIcon = {
                        Text(text = "🇪🇹", fontSize = 18.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_phone_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = loginPin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            loginPin = input
                        }
                    },
                    label = { Text("4-Digit Security PIN") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security PIN",
                            tint = GoldPrimary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { loginPinVisible = !loginPinVisible }) {
                            Icon(
                                imageVector = if (loginPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (loginPinVisible) "Hide PIN" else "Show PIN",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (loginPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { rememberMe = !rememberMe }
                            .testTag("remember_me_checkbox")
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoldPrimary,
                                checkmarkColor = BgDark,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Text(
                            text = "Remember Me",
                            fontFamily = PlusJakartaFontFamily,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            resetPhone = loginPhone
                            resetNewPin = ""
                            resetError = null
                            showResetPinDialog = true
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("reset_pin_button")
                    ) {
                        Text(
                            text = "Reset PIN",
                            fontFamily = OutfitFontFamily,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = authError,
                        fontFamily = PlusJakartaFontFamily,
                        color = OrangeDynamic,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("auth_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (!isSigningIn) {
                            onLoginSubmit(loginPhone, loginPin, rememberMe)
                        }
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("enter_lobby_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeDynamic,
                        disabledContainerColor = OrangeDynamic.copy(alpha = 0.6f)
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CONNECTING...",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "ENTER GAME LOBBY",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.6.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OrDivider()

                Spacer(modifier = Modifier.height(12.dp))

                GoogleSignInOutlineButton(
                    isSigningIn = isSigningIn,
                    onClick = onGoogleSignIn
                )
            } else {
                // CREATE ACCOUNT TAB
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x3320C997))
                        .border(1.dp, MintPrimary, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎁", fontSize = 26.sp, modifier = Modifier.padding(end = 10.dp))
                    Column {
                        Text(
                            text = "NEW PLAYER WELCOME BONUS",
                            fontFamily = OutfitFontFamily,
                            color = MintLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "Get +100 ETB Free Balance upon registration!",
                            fontFamily = PlusJakartaFontFamily,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name (e.g., Dawit Bekele)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Full Name",
                            tint = GoldPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = regPhone,
                    onValueChange = { regPhone = it },
                    label = { Text("Phone Number (+251 9...)") },
                    leadingIcon = {
                        Text(text = "🇪🇹", fontSize = 18.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "City / Region in Ethiopia",
                    fontFamily = PlusJakartaFontFamily,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ethiopianCities.forEach { city ->
                        val isCitySelected = selectedCity == city
                        Box(
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCitySelected) GoldPrimary else BgDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .clickable { selectedCity = city }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = city,
                                fontFamily = PlusJakartaFontFamily,
                                color = if (isCitySelected) BgDark else TextPrimary,
                                fontWeight = if (isCitySelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = regPin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            regPin = input
                        }
                    },
                    label = { Text("Set 4 to 6-Digit Security PIN") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Set Security PIN",
                            tint = GoldPrimary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { regPinVisible = !regPinVisible }) {
                            Icon(
                                imageVector = if (regPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (regPinVisible) "Hide PIN" else "Show PIN",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (regPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_pin_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    label = { Text("Referral / Promo Code (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_promo_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = authError,
                        fontFamily = PlusJakartaFontFamily,
                        color = OrangeDynamic,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("auth_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!isSigningIn) {
                            onCreateAccountSubmit(fullName, regPhone, selectedCity, regPin, promoCode)
                        }
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("create_account_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeDynamic,
                        disabledContainerColor = OrangeDynamic.copy(alpha = 0.6f)
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CREATING ACCOUNT...",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "CREATE ACCOUNT & CLAIM 100 ETB",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.4.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OrDivider()

                Spacer(modifier = Modifier.height(12.dp))

                GoogleSignInOutlineButton(
                    isSigningIn = isSigningIn,
                    onClick = onGoogleSignIn
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetPinDialog) {
        AlertDialog(
            onDismissRequest = { showResetPinDialog = false },
            containerColor = BgCardElevated,
            titleContentColor = GoldLight,
            textContentColor = TextPrimary,
            title = {
                Text(
                    text = "Reset Security PIN",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your Ethiopian mobile number and choose a new 4 to 6-digit PIN.",
                        fontFamily = PlusJakartaFontFamily,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = resetPhone,
                        onValueChange = {
                            resetPhone = it
                            resetError = null
                        },
                        label = { Text("Phone Number (+251 9... / 09...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resetNewPin,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                resetNewPin = input
                                resetError = null
                            }
                        },
                        label = { Text("New 4 to 6-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!resetError.isNullOrBlank()) {
                        Text(
                            text = resetError ?: "",
                            color = OrangeDynamic,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val err = onResetPin(resetPhone, resetNewPin)
                        if (err != null) {
                            resetError = err
                        } else {
                            loginPhone = resetPhone
                            loginPin = ""
                            showResetPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text(
                        text = "Save New PIN",
                        color = BgDark,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GoogleSignInOutlineButton(
    isSigningIn: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            if (!isSigningIn) onClick()
        },
        enabled = !isSigningIn,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("google_signin_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            disabledContainerColor = Color(0xCCFFFFFF)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "G",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = Color(0xFF4285F4)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign in with Google",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1F1F1F)
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = BorderColor
        )
        Text(
            text = "  OR SIGN IN WITH GOOGLE  ",
            fontFamily = PlusJakartaFontFamily,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = BorderColor
        )
    }
}
