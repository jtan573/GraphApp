package com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.R
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames

@Composable
fun AlertNearbyTroopersScreen() {
    val alertDetails = mapOf<String, String>(
        SchemaEventTypeNames.INCIDENT.key to "Mid-Flight Drone Propeller Failure",
        SchemaEventTypeNames.HOW.key to "Propeller blade sheared mid-flight due to material fatigue, causing crash into storage tent",
        SchemaEventTypeNames.WHEN.key to "2024-08-17 20:20:00",
        SchemaEventTypeNames.WHERE.key to "1.3901,103.8072"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, bottom = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Big red alert banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "ALERT",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Alert details
        alertDetails.forEach { (key, value) ->
            Column(
                modifier = Modifier.padding(bottom = 12.dp).padding(horizontal = 16.dp)
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E4E8C)
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.alert_trooper),
            contentDescription = "PNG display",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}
