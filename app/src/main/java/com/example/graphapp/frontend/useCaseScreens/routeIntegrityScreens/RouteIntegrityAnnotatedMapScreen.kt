package com.example.graphapp.frontend.useCaseScreens.routeIntegrityScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun RouteIntegrityAnnotatedMapScreen(
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Annotated Route Map",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        Text(
            text = "Map showing route and incidents affecting it:",
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
        )
        Image(
            painter = painterResource(id = R.drawable.route_integrity_map),
            contentDescription = "PNG display",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Button(
            onClick = { navController.navigate(NavItem.ReRouteIntegrityImage.route) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF45A0A8),          // background color
                contentColor = Color.White                   // text/icon color
            )
        ) {
            Text("Reroute to avoid disruptions", fontSize = 16.sp)
        }
    }
}