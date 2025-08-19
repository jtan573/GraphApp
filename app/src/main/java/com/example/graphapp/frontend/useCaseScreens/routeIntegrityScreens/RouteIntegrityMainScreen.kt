package com.example.graphapp.frontend.useCaseScreens.routeIntegrityScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.R
import com.example.graphapp.frontend.navigation.NavItem

@Composable
fun RouteIntegrityMainScreen(navController: NavController){
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Operational Route",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
        }
        Text(
            text = "Map with route coordinates:",
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
        )
        Image(
            painter = painterResource(id = R.drawable.original_route_map),
            contentDescription = "PNG display",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Button(
            onClick = { navController.navigate(NavItem.RouteIntegrityImage.route) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF45A0A8),          // background color
                contentColor = Color.White                   // text/icon color
            )
        ) {
            Text("Check if route is operational >", fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RouteIntegrityMainPreview() {
    val dummyNavController = rememberNavController()
    RouteIntegrityMainScreen(dummyNavController)
}