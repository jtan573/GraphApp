package com.example.graphapp.frontend.useCaseScreens.suspiciousBehaviourScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.R
import com.example.graphapp.frontend.navigation.NavItem

@Composable
fun SuspiciousActivityByLocationScreen(
    navController: NavController
) {

    val taskInfo = mapOf<String, String>(
        "Task" to "Investigate series of Suspicious Events",
        "Objective" to "Conduct a systematic sweep of the area to identify potential threats, unusual behavior, or unattended objects."
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {

        Text(
            text = "Task Details",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 12.dp, start = 10.dp),
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF9DCFD4)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // optional, no shadow
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "TASK ASSIGNED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    taskInfo.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "$label:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E4E8C)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }

                }
            }
            Image(
                painter = painterResource(id = R.drawable.suspicious_behaviour_location),
                contentDescription = "PNG display",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate(NavItem.SuspiciousEventsDetailScreen.route) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF45A0A8),          // background color
                    contentColor = Color.White                   // text/icon color
                )
            ) {
                Text("Check Details and other Relevant Info >", fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SuspiciousActivityMapPreview() {
    val dummyNavController = rememberNavController()
    SuspiciousActivityByLocationScreen(dummyNavController)
}