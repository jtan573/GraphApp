package com.example.graphapp.ui.screens

import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.graphapp.ui.viewmodels.GraphViewModel
import org.json.JSONObject

@Composable
fun GraphViewScreen(viewModel: GraphViewModel) {
    val graphJson by viewModel.graphData.collectAsState()

    var showNodeForm by remember { mutableStateOf(false) }
    var showEdgeForm by remember { mutableStateOf(false) }

    // Node input state
    var nodeName by remember { mutableStateOf("") }
    var nodeType by remember { mutableStateOf("") }

    // Edge input state
    var fromNode by remember { mutableStateOf("") }
    var toNode by remember { mutableStateOf("") }
    var relationType by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "GraphApp",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top=64.dp, bottom=8.dp),
            )
            Box {
                Row (
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Button(
                        onClick = { showNodeForm = !showNodeForm },
                        modifier = Modifier.padding(end = 3.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(if (showNodeForm) "Hide" else "+ Node", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showEdgeForm = !showEdgeForm },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(if (showEdgeForm) "Hide" else "+ Edge", fontSize = 12.sp)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showNodeForm) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = "Insert Node:")

                OutlinedTextField(
                    value = nodeName,
                    onValueChange = { nodeName = it },
                    label = { Text("Node Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nodeType,
                    onValueChange = { nodeType = it },
                    label = { Text("Node Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.insertOneNode(nodeName, nodeType)
                        nodeName = ""
                        nodeType = ""
                        showNodeForm = false
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Add Node")
                }
            }
        }

// Edge Form
        AnimatedVisibility(visible = showEdgeForm) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = "Insert Edge:")

                OutlinedTextField(
                    value = fromNode,
                    onValueChange = { fromNode = it },
                    label = { Text("From Node") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = toNode,
                    onValueChange = { toNode = it },
                    label = { Text("To Node") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationType,
                    onValueChange = { relationType = it },
                    label = { Text("Relation Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.insertOneEdge(fromNode, toNode, relationType)
                        fromNode = ""
                        toNode = ""
                        relationType = ""
                        showEdgeForm = false
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Add Edge")
                }
            }
        }

        if (graphJson != null) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        this.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.settings.javaScriptEnabled = true
                        this.isHorizontalScrollBarEnabled = true
                        this.isVerticalScrollBarEnabled = true
                        this.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                evaluateJavascript("loadGraph(${JSONObject.quote(graphJson)});") { result ->
                                    Log.d("GraphRelations", "JavaScript executed: $result")
                                }
                            }
                        }
                        loadUrl("file:///android_asset/graph.html")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                update = { webView ->
//                    it.loadUrl("file:///android_asset/graph.html")
                    graphJson?.let {
                        Log.d("WebView", "Injecting graph: $it")
                        webView.evaluateJavascript("loadGraph(${JSONObject.quote(it)});") { result ->
                            Log.d("GraphWebView", "JS Result: $result")
                        }
                    }
                }
            )
        } else {
            Text(
                text = "Loading graph...",
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}