package com.example.graphapp.ui.screens

import android.R.attr.onClick
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.graphapp.ui.navigation.NavItem
import com.example.graphapp.ui.viewmodels.GraphViewModel

@Composable
fun UseCaseScreen(
    viewModel: GraphViewModel,
    navController: NavHostController,
) {
    val useCasesList = mapOf<String, () -> Unit>(
        "Find Relevant Personnel" to { navController.navigate(NavItem.Personnel.route) },
        "Threat Detections and Response" to { navController.navigate(NavItem.Events.route) },
//        "Suspicious Behaviour Prediction",
//        "Smart Patrol Routing",
//        "Threat Intelligence Correlation",
//        "Personnel Coordination"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Use Cases",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 2 columns
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(useCasesList.size) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable {
                            useCasesList.values.elementAt(index).invoke()
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(text = useCasesList.keys.elementAt(index))
                    }
                }
            }
        }
    }
}
