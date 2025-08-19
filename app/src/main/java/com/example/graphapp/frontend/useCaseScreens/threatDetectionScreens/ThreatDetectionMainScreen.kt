package com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.frontend.navigation.NavItem

@Composable
fun ThreatDetectionMainScreen(navController: NavController) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Threat Alert & Response",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
        }

        // ALERT SCREEN
        Card(
            onClick = { navController.navigate(NavItem.AlertTroopersScreen.route) },
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp).padding(top = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF96D3D9) // light red background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ALERT TROOPERS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Arrow Right",
                    tint = Color.Black
                )
            }
        }

        Card(
            onClick = { navController.navigate(NavItem.ReceivedTaskScreen.route) },
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp).padding(top = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF96D3D9) // light red background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TASK ASSIGNMENT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Arrow Right",
                    tint = Color.Black
                )
            }
        }


    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ThreatDetectionMainScreenPreview() {
    val dummyNavController = rememberNavController()
    ThreatDetectionMainScreen(navController = dummyNavController)
}