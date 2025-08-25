package com.example.graphapp.frontend.useCaseScreens.relevantPersonnelScreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.graphapp.R
import com.example.graphapp.frontend.components.PersonnelSearchForm
import com.example.graphapp.frontend.viewmodels.GraphViewModel
import kotlinx.coroutines.launch

@Composable
fun RelevantPersonnelMainScreen(
    viewModel: GraphViewModel,
    navController: NavController
) {
    var localUserQueryDescription by remember { mutableStateOf("Looking for support to clear launch pad and inspect drone fleets.") }
    var localUserQueryLocation by remember { mutableStateOf("1.3901,103.8072") }
    var lastSubmittedDescription by remember { mutableStateOf("") }
    var lastSubmittedLocation by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(true) }

    val contactState by viewModel.relevantContactState.collectAsState()

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
                text = "Personnel Search",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
            Button(
                onClick = { showForm = !showForm },
                enabled = !isLoading,
                modifier = Modifier.padding(end = 3.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (showForm) "Hide" else "Search",
                        fontSize = 12.sp
                    )
                }
            }
        }

        AnimatedVisibility(visible = showForm) {
            PersonnelSearchForm(
                description = localUserQueryDescription,
                onDescriptionChange = { localUserQueryDescription = it },
                location = localUserQueryLocation,
                onLocationChange = { localUserQueryLocation = it },
                onSubmit = {
                    coroutineScope.launch {
                        isLoading = true
                        lastSubmittedDescription = localUserQueryDescription
                        lastSubmittedLocation = localUserQueryLocation
                        viewModel.findRelevantPersonnelOnDemand(
                            inputLocation = localUserQueryLocation,
                            inputDescription = localUserQueryDescription
                        )
                        isLoading = false
                        showForm = false
                    }
                }
            )

        }
        contactState?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (contactState?.isEmpty() == true) {
                    Text(
                        text = "No active personnel within 3000m radius is found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 10.dp)
                    )
                } else {
                    Text(
                        text = "Search Results:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    contactState?.forEach { (user, distance) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF9CCEDB)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Distance row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box() {
                                        Row(verticalAlignment = Alignment.CenterVertically,) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = "${distance}m away",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp).clickable { /* onClick */ }
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Identifier + Role
                                Text(
                                    text = "${user.identifier} · ${user.role}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(Modifier.height(4.dp))

                                // Description
                                Text(
                                    text = user.specialisation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(5.dp))


                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Image(
                    painter = painterResource(id = R.drawable.contact_personnel_results),
                    contentDescription = "PNG display",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }


    }
}