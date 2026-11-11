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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.OrangeDynamic
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class AuthTab {
    LOGIN,
    SIGNUP
}

@Composable
fun AuthScreen(
    onLogin: (phoneOrUsername: String, pin: String) -> Unit,
    onRegister: (fullName: String, phone: String, city: String, pin: String, promoCode: String) -> Unit,
    onGoogleSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AuthTab.LOGIN) }

    // Login Form State
    var loginPhone by remember { mutableStateOf("") }
    var loginPin by remember { mutableStateOf("") }
    var loginPinVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Sign Up Form State
    var regFullName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regCity by remember { mutableStateOf("Addis Ababa") }
    var regPin by remember { mutableStateOf("") }
    var regPromo by remember { mutableStateOf("") }
    var regPinVisible by remember { mutableStateOf(false) }
    var regError by remember { mutableStateOf<String?>(null) }

    val ethiopianCities = listOf("Addis Ababa", "Hawassa", "Bahir Dar", "Dire Dawa", "Adama", "Mekelle", "Gondar")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Brand Hero: Glowing Ball + Title
        Box(
            modifier = Modifier
                .size(64.dp)
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
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "YEM BINGO",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = GoldLight
        )

        Text(
            text = "ETHIOPIA'S #1 LIVE MULTIPLAYER",
            color = MintPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        Text(
            text = "Play Live with Ethiopian Players across Addis & Regions",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // Pill Segmented Tab Control: Log In vs Create Account
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == AuthTab.LOGIN) GoldPrimary else Color.Transparent)
                    .clickable {
                        selectedTab = AuthTab.LOGIN
                        loginError = null
                    }
                    .padding(vertical = 10.dp)
                    .testTag("tab_login"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOG IN",
                    color = if (selectedTab == AuthTab.LOGIN) BgDark else TextSecondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == AuthTab.SIGNUP) GoldPrimary else Color.Transparent)
                    .clickable {
                        selectedTab = AuthTab.SIGNUP
                        regError = null
                    }
                    .padding(vertical = 10.dp)
                    .testTag("tab_signup"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CREATE ACCOUNT",
                    color = if (selectedTab == AuthTab.SIGNUP) BgDark else TextSecondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // TAB 1: LOG IN
        if (selectedTab == AuthTab.LOGIN) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCardElevated)
                    .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Welcome Back!",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Enter your registered Ethiopian mobile number and PIN",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Phone Input
                OutlinedTextField(
                    value = loginPhone,
                    onValueChange = {
                        loginPhone = it
                        loginError = null
                    },
                    label = { Text("Phone Number (+251 9... / 09...)") },
                    leadingIcon = {
                        Text(
                            text = "🇪🇹",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_phone_input"),
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

                // PIN Input
                OutlinedTextField(
                    value = loginPin,
                    onValueChange = {
                        loginPin = it
                        loginError = null
                    },
                    label = { Text("4-Digit Security PIN") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "PIN", tint = GoldPrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { loginPinVisible = !loginPinVisible }) {
                            Icon(
                                imageVector = if (loginPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle PIN visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (loginPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input"),
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

                // Remember me & Forgot PIN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoldPrimary,
                                checkmarkColor = BgDark,
                                uncheckedColor = BorderColor
                            )
                        )
                        Text(
                            text = "Remember Me",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "Reset PIN",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                loginPin = "7777"
                                loginError = "PIN reset to demo default 7777"
                            }
                            .padding(4.dp)
                    )
                }

                // Error Message
                if (loginError != null) {
                    Text(
                        text = loginError ?: "",
                        color = OrangeDynamic,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Log In Button
                Button(
                    onClick = {
                        if (loginPhone.isBlank()) {
                            loginError = "Please enter your phone number"
                        } else if (loginPin.length < 4) {
                            loginError = "Please enter your 4-digit PIN"
                        } else {
                            onLogin(loginPhone, loginPin)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_login_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
                ) {
                    Text(
                        text = "ENTER GAME LOBBY",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Google Sign-In Button
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("google_signin_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G ",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFF4285F4)
                        )
                        Text(
                            text = "Sign in with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1F1F1F)
                        )
                    }
                }
            }
        }


        // TAB 2: CREATE ACCOUNT
        if (selectedTab == AuthTab.SIGNUP) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCardElevated)
                    .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                // Free Welcome Bonus Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3320C997))
                        .border(1.dp, MintPrimary, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎁", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(
                            text = "NEW PLAYER WELCOME BONUS",
                            color = MintLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Get +100 ETB Free Balance upon registration!",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Full Name
                OutlinedTextField(
                    value = regFullName,
                    onValueChange = {
                        regFullName = it
                        regError = null
                    },
                    label = { Text("Full Name (e.g., Dawit Bekele)") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Name", tint = GoldPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input"),
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

                // Phone
                OutlinedTextField(
                    value = regPhone,
                    onValueChange = {
                        regPhone = it
                        regError = null
                    },
                    label = { Text("Phone Number (+251 9...)") },
                    leadingIcon = {
                        Text(
                            text = "🇪🇹",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone_input"),
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

                // City Chips Selector
                Text(
                    text = "City / Region in Ethiopia",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ethiopianCities.forEach { city ->
                        val isCitySelected = regCity == city
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCitySelected) GoldPrimary else BgDark)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { regCity = city }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = city,
                                color = if (isCitySelected) BgDark else TextPrimary,
                                fontWeight = if (isCitySelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Create Security PIN
                OutlinedTextField(
                    value = regPin,
                    onValueChange = {
                        regPin = it.filter { c -> c.isDigit() }.take(6)
                        regError = null
                    },
                    label = { Text("Set 4 to 6-Digit Security PIN") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "PIN", tint = GoldPrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { regPinVisible = !regPinVisible }) {
                            Icon(
                                imageVector = if (regPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle PIN visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (regPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_pin_input"),
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

                // Optional Promo / Referral code
                OutlinedTextField(
                    value = regPromo,
                    onValueChange = { regPromo = it.uppercase() },
                    label = { Text("Referral / Promo Code (Optional)") },
                    placeholder = { Text("e.g. YEM-Yemane-77") },
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

                // Error Message
                if (regError != null) {
                    Text(
                        text = regError ?: "",
                        color = OrangeDynamic,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Register Button
                Button(
                    onClick = {
                        when {
                            regFullName.trim().length < 2 -> regError = "Please enter your full name"
                            regPhone.trim().length < 9 -> regError = "Please enter a valid Ethiopian phone number"
                            regPin.trim().length < 4 -> regError = "Security PIN must be at least 4 digits"
                            else -> onRegister(regFullName.trim(), regPhone.trim(), regCity, regPin.trim(), regPromo.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_register_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeDynamic)
                ) {
                    Text(
                        text = "CREATE ACCOUNT & CLAIM 100 ETB",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("google_signup_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G ",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFF4285F4)
                        )
                        Text(
                            text = "Sign up with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1F1F1F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "By creating an account, you confirm you are 18+ years of age and agree to Yem Bingo Fair Play rules.",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
