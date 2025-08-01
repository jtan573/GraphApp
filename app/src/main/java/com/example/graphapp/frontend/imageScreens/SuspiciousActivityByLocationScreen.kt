package com.example.graphapp.frontend.imageScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.graphapp.R

@Composable
fun SuspiciousActivityByLocationScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Suspicious Activities Map",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        Text(
            text = "Locations with suspicious activities:",
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
        )
        Image(
            painter = painterResource(id = R.drawable.suspicious_behaviour_location),
            contentDescription = "PNG display",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}