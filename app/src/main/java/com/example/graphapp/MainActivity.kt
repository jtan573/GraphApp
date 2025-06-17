package com.example.graphapp

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.graphapp.ui.theme.GraphAppTheme
import com.example.graphapp.ui.viewmodels.GraphViewModel
import org.json.JSONObject


class MainActivity : ComponentActivity() {
    private val viewModel: GraphViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GraphAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GraphRelations(viewModel)
                }
            }
        }
    }
}

@Composable
fun GraphRelations(viewModel: GraphViewModel) {
    val context = LocalContext.current
    val graphJson by viewModel.graphData.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (graphJson != null) {
            Log.d("GraphViewModel", "Generated JSON: $graphJson")
            Text(
                text = "Graph here...",
                modifier = Modifier
                    .padding(top=64.dp, bottom=8.dp)
                    .align(Alignment.CenterHorizontally)
            )

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
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                update = {
                    it.loadUrl("file:///android_asset/graph.html")
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
