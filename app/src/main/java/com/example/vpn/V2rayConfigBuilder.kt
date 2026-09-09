package com.example.vpn

import android.util.Base64
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object V2rayConfigBuilder {

    /**
     * Converts a raw VPN URI (vless://, vmess://, trojan://, ss://)
     * into a complete Xray/V2Ray client configuration JSON string.
     */
    fun buildConfig(rawUri: String): String {
        val outbound = when {
            rawUri.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(rawUri)
            rawUri.startsWith("vless://", ignoreCase = true) -> parseVless(rawUri)
            rawUri.startsWith("vmess://", ignoreCase = true) -> parseVmess(rawUri)
            rawUri.startsWith("trojan://", ignoreCase = true) -> parseTrojan(rawUri)
            else -> throw IllegalArgumentException("Unsupported protocol: $rawUri")
        }

        val directOutbound = JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
            put("settings", JSONObject())
        }

        val inbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks")
                put("port", 10808)
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                    })
                })
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
            })
        }

        val dns = JSONObject().apply {
            put("servers", JSONArray().apply {
                put("8.8.8.8")
                put("1.1.1.1")
            })
        }

        val routing = JSONObject().apply {
            put("domainStrategy", "AsIs")
            put("rules", JSONArray().apply {
                // Force all traffic through the proxy outbound by default.
                put(JSONObject().apply {
                    put("type", "field")
                    put("outboundTag", "proxy")
                    put("port", "0-65535")
                })
            })
        }

        val root = JSONObject().apply {
            put("log", JSONObject().apply {
                put("loglevel", "warning")
            })
            put("dns", dns)
            put("inbounds", inbounds)
            put("outbounds", JSONArray().apply {
                put(outbound)
                put(directOutbound)
            })
            put("routing", routing)
        }

        return root.toString(2)
    }

    /**
     * Parses Shadowsocks URI:
     * ss://BASE64(method:password)@server:port#tag
     * OR ss://BASE64(method:password@server:port)#tag
     */
    private fun parseShadowsocks(rawUri: String): JSONObject {
        var cleanUri = rawUri.removePrefix("ss://")
        val hashIdx = cleanUri.indexOf('#')
        if (hashIdx != -1) {
            cleanUri = cleanUri.substring(0, hashIdx)
        }
        val qIdx = cleanUri.indexOf('?')
        if (qIdx != -1) {
            cleanUri = cleanUri.substring(0, qIdx)
        }

        var method = "chacha20-ietf-poly1305"
        var password = ""
        var host = ""
        var port = 8388

        if (cleanUri.contains("@")) {
            val parts = cleanUri.split("@", limit = 2)
            val userInfo = decodeBase64Safe(parts[0])
            if (userInfo.contains(":")) {
                val uParts = userInfo.split(":", limit = 2)
                method = uParts[0]
                password = uParts[1]
            }
            val hostPort = parts[1].split(":", limit = 2)
            host = hostPort[0]
            port = hostPort.getOrNull(1)?.toIntOrNull() ?: 8388
        } else {
            val decoded = decodeBase64Safe(cleanUri)
            val atIdx = decoded.lastIndexOf('@')
            if (atIdx != -1) {
                val userInfo = decoded.substring(0, atIdx)
                val hostPort = decoded.substring(atIdx + 1)
                val uParts = userInfo.split(":", limit = 2)
                method = uParts[0]
                password = uParts.getOrElse(1) { "" }
                val hpParts = hostPort.split(":", limit = 2)
                host = hpParts[0]
                port = hpParts.getOrNull(1)?.toIntOrNull() ?: 8388
            }
        }

        val serverObj = JSONObject().apply {
            put("address", host)
            put("port", port)
            put("method", method)
            put("password", password)
            put("ota", false)
        }

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "shadowsocks")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply { put(serverObj) })
            })
            put("streamSettings", JSONObject().apply {
                put("network", "tcp")
            })
        }
    }

    /**
     * Parses VLESS URI:
     * vless://uuid@host:port?encryption=none&security=tls&type=tcp...#tag
     */
    private fun parseVless(rawUri: String): JSONObject {
        val uri = Uri.parse(rawUri)
        val uuid = uri.userInfo ?: ""
        val host = uri.host ?: ""
        val port = if (uri.port != -1) uri.port else 443
        val security = uri.getQueryParameter("security") ?: "none"
        val type = uri.getQueryParameter("type") ?: "tcp"
        val flow = uri.getQueryParameter("flow") ?: ""
        val sni = uri.getQueryParameter("sni") ?: host

        val userObj = JSONObject().apply {
            put("id", uuid)
            put("encryption", uri.getQueryParameter("encryption") ?: "none")
            if (flow.isNotEmpty()) put("flow", flow)
        }

        val streamSettings = JSONObject().apply {
            put("network", type)
            put("security", security)
            if (security == "tls" || security == "reality") {
                put("tlsSettings", JSONObject().apply {
                    put("serverName", sni)
                    put("allowInsecure", false)
                })
            }
        }

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", host)
                        put("port", port)
                        put("users", JSONArray().apply { put(userObj) })
                    })
                })
            })
            put("streamSettings", streamSettings)
        }
    }

    /**
     * Parses Trojan URI:
     * trojan://password@host:port?security=tls&sni=...#tag
     */
    private fun parseTrojan(rawUri: String): JSONObject {
        val uri = Uri.parse(rawUri)
        val password = uri.userInfo ?: ""
        val host = uri.host ?: ""
        val port = if (uri.port != -1) uri.port else 443
        val security = uri.getQueryParameter("security") ?: "tls"
        val sni = uri.getQueryParameter("sni") ?: host

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "trojan")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", host)
                        put("port", port)
                        put("password", password)
                    })
                })
            })
            put("streamSettings", JSONObject().apply {
                put("network", uri.getQueryParameter("type") ?: "tcp")
                put("security", security)
                put("tlsSettings", JSONObject().apply {
                    put("serverName", sni)
                    put("allowInsecure", false)
                })
            })
        }
    }

    /**
     * Parses VMess URI (JSON encoded in Base64):
     * vmess://BASE64_JSON
     */
    private fun parseVmess(rawUri: String): JSONObject {
        val b64 = rawUri.removePrefix("vmess://").trim()
        val jsonStr = decodeBase64Safe(b64)
        val vObj = JSONObject(jsonStr)

        val host = vObj.optString("add")
        val port = vObj.optInt("port", 443)
        val id = vObj.optString("id")
        val aid = vObj.optInt("aid", 0)
        val net = vObj.optString("net", "tcp")
        val type = vObj.optString("type", "none")
        val tls = vObj.optString("tls", "none")
        val sni = vObj.optString("sni", host)

        val userObj = JSONObject().apply {
            put("id", id)
            put("alterId", aid)
            put("security", "auto")
        }

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vmess")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", host)
                        put("port", port)
                        put("users", JSONArray().apply { put(userObj) })
                    })
                })
            })
            put("streamSettings", JSONObject().apply {
                put("network", net)
                put("security", if (tls == "tls") "tls" else "none")
            })
        }
    }

    private fun decodeBase64Safe(input: String): String {
        return try {
            val clean = input.replace("-", "+").replace("_", "/")
            val padded = when (clean.length % 4) {
                2 -> "$clean=="
                3 -> "$clean="
                else -> clean
            }
            String(Base64.decode(padded, Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            input
        }
    }
}
