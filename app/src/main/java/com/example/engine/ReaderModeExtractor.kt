package com.example.engine

import android.webkit.WebView
import org.json.JSONObject

data class ReaderArticle(
    val title: String,
    val siteName: String,
    val contentHtml: String,
    val textLength: Int
)

object ReaderModeExtractor {

    private val EXTRACTION_SCRIPT = """
        (function() {
            var title = document.title || '';
            var h1 = document.querySelector('h1');
            if (h1 && h1.innerText) title = h1.innerText;

            var siteName = location.hostname.replace('www.', '');

            var article = document.querySelector('article') || document.querySelector('main') || document.querySelector('.post-content') || document.body;
            var paragraphs = article.querySelectorAll('p, h2, h3, h4, blockquote, ul, ol');
            
            var content = '';
            var count = 0;
            paragraphs.forEach(function(p) {
                var text = p.innerText.trim();
                if (text.length > 20) {
                    content += '<p style="margin-bottom: 1.2em; line-height: 1.7;">' + text + '</p>';
                    count += text.length;
                }
            });

            return JSON.stringify({
                title: title,
                siteName: siteName,
                contentHtml: content,
                textLength: count
            });
        })();
    """.trimIndent()

    fun extractArticle(webView: WebView, onResult: (ReaderArticle?) -> Unit) {
        webView.evaluateJavascript(EXTRACTION_SCRIPT) { rawJson ->
            if (rawJson == null || rawJson == "null") {
                onResult(null)
                return@evaluateJavascript
            }
            try {
                // evaluateJavascript returns a JSON-encoded string
                val unquoted = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                    JSONObject("{ \"res\": $rawJson }").getString("res")
                } else rawJson
                val obj = JSONObject(unquoted)
                val article = ReaderArticle(
                    title = obj.optString("title", "Article"),
                    siteName = obj.optString("siteName", ""),
                    contentHtml = obj.optString("contentHtml", ""),
                    textLength = obj.optInt("textLength", 0)
                )
                onResult(article)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}
