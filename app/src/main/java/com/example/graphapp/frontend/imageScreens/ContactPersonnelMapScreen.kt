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
fun ContactPersonnelMapScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Nearby Personnel Map",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        Text(
            text = "Relevant personnel close to you:",
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
        )
        Image(
            painter = painterResource(id = R.drawable.contact_personnel_results),
            contentDescription = "PNG display",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}