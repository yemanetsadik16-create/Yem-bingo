package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BlueHeader
import com.example.ui.theme.BorderColor
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.PurpleHeader
import com.example.ui.theme.RedHeader
import com.example.ui.theme.TextTertiary

data class MatrixRowData(
    val letter: String,
    val badgeColor: Color,
    val range: IntRange
)

@Composable
fun Matrix100Grid(
    calledNumbers: List<Int>,
    latestCalledBall: Int?,
    modifier: Modifier = Modifier
) {
    val calledSet = calledNumbers.toSet()

    val rowsData = listOf(
        MatrixRowData("B", RedHeader, 1..15),
        MatrixRowData("I", BlueHeader, 16..30),
        MatrixRowData("N", GoldPrimary, 31..45),
        MatrixRowData("G", MintPrimary, 46..60),
        MatrixRowData("O", PurpleHeader, 61..75)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCardElevated)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(10.dp)
            .testTag("matrix_75_grid")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE 1–75 NUMBERS MATRIX",
                color = GoldPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MintPrimary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Updated Live",
                        color = BgDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5 rows: B (1-15), I (16-30), N (31-45), G (46-60), O (61-75)
        rowsData.forEach { rowData ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Letter badge on left
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(rowData.badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rowData.letter,
                        color = if (rowData.letter == "N") BgDark else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }

                // 15 numbers in this row
                rowData.range.forEach { number ->
                    val isLatest = number == latestCalledBall
                    val isCalled = calledSet.contains(number)

                    val cellBg = when {
                        isLatest -> GoldPrimary
                        isCalled -> MintDark
                        else -> BgDark
                    }

                    val borderColor = when {
                        isLatest -> Color.White
                        isCalled -> MintLight
                        else -> Color(0x331A4755)
                    }

                    val textColor = when {
                        isLatest -> BgDark
                        isCalled -> Color.White
                        else -> TextTertiary
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cellBg)
                            .border(width = 0.5.dp, color = borderColor, shape = RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString(),
                            color = textColor,
                            fontWeight = if (isLatest || isCalled) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}
