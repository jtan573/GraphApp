package com.example.graphapp.frontend.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun GraphWebView(
    graphJson: String?,
    selectedFilter: String,
    modifier: Modifier = Modifier,
    htmlFileName: String = "eventGraph.html",
) {
    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                isHorizontalScrollBarEnabled = true
                isVerticalScrollBarEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        evaluateJavascript(
                            "loadGraph(${JSONObject.quote(graphJson)}, '${selectedFilter}')"
                        ) { result ->
                            Log.d("GraphRelations", "JavaScript executed: $result")
                        }
                    }
                }
                loadUrl("file:///android_asset/$htmlFileName")
            }
        },
        modifier = modifier,
        update = { webView ->
            graphJson?.let {
                Log.d("WebView", "Injecting graph: $it")
                webView.evaluateJavascript("loadGraph(${JSONObject.quote(it)}, '${selectedFilter}');") { result ->
                    Log.d("GraphWebView", "JS Result: $result")
                }
            }
        }
    )
}
