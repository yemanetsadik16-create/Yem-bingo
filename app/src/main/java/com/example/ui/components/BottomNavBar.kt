package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.NavTab

@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Color(0xF20A2229))
            .border(width = 1.dp, color = BorderColor)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            title = "Play",
            icon = Icons.Filled.PlayCircle,
            isSelected = selectedTab == NavTab.PLAY,
            onClick = { onTabSelected(NavTab.PLAY) },
            testTag = "nav_play"
        )
        NavItem(
            title = "Tournament",
            icon = Icons.Filled.EmojiEvents,
            isSelected = selectedTab == NavTab.TOURNAMENT,
            onClick = { onTabSelected(NavTab.TOURNAMENT) },
            testTag = "nav_tournament"
        )
        NavItem(
            title = "Wallet & Profile",
            icon = Icons.Filled.Person,
            isSelected = selectedTab == NavTab.PROFILE,
            onClick = { onTabSelected(NavTab.PROFILE) },
            testTag = "nav_profile"
        )
    }
}

@Composable
private fun NavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tint = if (isSelected) GoldPrimary else TextSecondary

    Column(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}
