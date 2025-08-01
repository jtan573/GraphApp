package com.example.graphapp.frontend.imageScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.graphapp.R
import com.example.graphapp.frontend.navigation.NavItem

@Composable
fun ReRouteIntegrityAnnotatedMapScreen(
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Annotated Route Map (Re-Route)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        Text(
            text = "New route to avoid disruptions:",
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
        )
        Image(
            painter = painterResource(id = R.drawable.reroute_integrity_map),
            contentDescription = "PNG display",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Row (
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Button(
                onClick = { navController.navigate(NavItem.RouteIntegrityImage.route) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Back")
            }
        }
    }
}