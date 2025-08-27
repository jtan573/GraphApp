package com.example.graphapp.frontend.useCaseScreens.shiftHandoverScreens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.graphapp.data.db.ActionNodeEntity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.frontend.navigation.NavItem
import com.example.graphapp.frontend.useCaseScreens.formatMillisToSGT
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ShiftHandoverMainScreen() {

    val base = 1_694_761_200_000L // e.g., 2023-09-15 10:20:00Z
    val userActions = listOf(
        ActionNodeEntity(
            module = "", action = "",
            content = "Monitored drone surveillance feed",
            timestamp = base +   0 * 60_000
        ),
        ActionNodeEntity(
            module = "", action = "",
            content = "Reviewed summary of active incidents", timestamp = base + 10 * 60_000
        ),
        ActionNodeEntity(
            module = "", action = "",
            content = "Assigned with task as quick reaction force to hotspot", timestamp = base + 25 * 60_000
        ),
        ActionNodeEntity(
            module = "", action = "",
            content = "Reviewed logistics status from SPC-004", timestamp = base + 55 * 60_000
        ),
    )
    val user = UserNodeEntity(
        identifier = "CPT-006",
        role = "Operations Officer",
        specialisation = "Manages area surveillance operations using integrated aerial and ground assets.",
        currentLocation = "1.3400,103.6900"
    )
    val sorted = remember(userActions) { userActions.sortedBy { it.timestamp } }

    val shiftSummary =
        "CPT-006 monitored drone surveillance for situational awareness, reviewed active incidents, deployed as Quick Reaction Force to a hotspot, and confirmed logistics readiness with SPC-004."


    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shift Summary",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent),
                border = BorderStroke(width = 3.dp, color = Color(0xFF9CCEDB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${user.identifier} · ${user.role}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = user.specialisation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        itemsIndexed(sorted) { i, action ->
            TimelineNode(position = if (i == 0) {
                TimelineNodePosition.FIRST
            } else if (i == (sorted.size - 1)) {
                TimelineNodePosition.LAST
            } else {
                TimelineNodePosition.MIDDLE
            }, action)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF9CCEDB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Handover Summary",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = shiftSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ActionsTimelinePreview() {
    ShiftHandoverMainScreen()
}

@Composable
fun TimelineNode(
    position: TimelineNodePosition,
    action: ActionNodeEntity
) {
    Box(
        modifier = Modifier.fillMaxWidth().fillMaxSize()
            .padding(horizontal = 16.dp)
            .drawBehind {
                val circleRadiusInPx = (10.dp).toPx()
                drawCircle(
                    color = Color(0xFF9CCEDB),
                    radius = circleRadiusInPx,
                    center = Offset(circleRadiusInPx, circleRadiusInPx)
                )
                if (position != TimelineNodePosition.LAST) {
                    drawLine(
                        color = Color(0xFF9CCEDB),
                        start = Offset(x = circleRadiusInPx, y = circleRadiusInPx * 2),
                        end = Offset(x = circleRadiusInPx, y = size.height),
                        strokeWidth = (3.dp).toPx()
                    )
                }
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 32.dp, bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column() {
                Text(
                    text = formatMillisToSGT(action.timestamp.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(5.dp))
                Text(text = action.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

enum class TimelineNodePosition {
    FIRST, MIDDLE, LAST
}

