package com.v2ray.ang.handler

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the user's own subscription from the Cloudflare Worker.
 *
 * The Worker returns a text/plain Base64 subscription at /sub. Decoded
 * entries are passed to the app's existing batch-config importer so the
 * normal v2rayNG parsing and profile storage remain in use.
 */
object WorkerConfigFetcher {
    const val WORKER_BASE_URL = "https://super-frog-38cf.fart27962.workers.dev/"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024

    data class Result(
        val configsText: String,
        val configCount: Int,
    )

    suspend fun fetch(): Result = withContext(Dispatchers.IO) {
        val endpoint = URL(WORKER_BASE_URL.trimEnd('/') + "/sub")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "text/plain")
            setRequestProperty("Cache-Control", "no-cache")
            instanceFollowRedirects = true
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("Worker returned HTTP $status")
            }

            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) {
                        throw IllegalStateException("Subscription response is too large")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }

            val body = bytes.toString(Charsets.UTF_8).trim()
            val decoded = if (
                connection.getHeaderField("X-Subscription-Format")
                    ?.equals("base64", ignoreCase = true) == true ||
                !body.startsWith("vless://", ignoreCase = true) &&
                !body.startsWith("vmess://", ignoreCase = true) &&
                !body.startsWith("trojan://", ignoreCase = true)
            ) {
                val normalized = body.filterNot { it.isWhitespace() }
                try {
                    String(Base64.decode(normalized, Base64.DEFAULT), Charsets.UTF_8)
                } catch (error: IllegalArgumentException) {
                    throw IllegalStateException("Worker response is not valid Base64 subscription data", error)
                }
            } else {
                body
            }

            val entries = decoded.lineSequence()
                .map { it.trim() }
                .filter { line ->
                    line.startsWith("vless://", true) ||
                    line.startsWith("vmess://", true) ||
                    line.startsWith("trojan://", true)
                }
                .toList()

            if (entries.isEmpty()) {
                throw IllegalStateException("Worker returned no supported VLESS, VMess, or Trojan configs")
            }

            Result(entries.joinToString("\n"), entries.size)
        } finally {
            connection.disconnect()
        }
    }
}
