package com.example.graphapp.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun GraphWebView(graphJson: String?, modifier: Modifier = Modifier) {
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
                        evaluateJavascript("loadGraph(${JSONObject.quote(graphJson)});") { result ->
                            Log.d("GraphRelations", "JavaScript executed: $result")
                        }
                    }
                }
                loadUrl("file:///android_asset/graph.html")
            }
        },
        modifier = modifier,
        update = { webView ->
            graphJson?.let {
                Log.d("WebView", "Injecting graph: $it")
                webView.evaluateJavascript("loadGraph(${JSONObject.quote(it)});") { result ->
                    Log.d("GraphWebView", "JS Result: $result")
                }
            }
        }
    )
}
