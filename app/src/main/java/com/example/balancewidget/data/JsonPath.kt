package com.example.balancewidget.data

import org.json.JSONArray
import org.json.JSONObject

object JsonPath {
    private val segmentPattern = Regex("^([^\\[]+)(?:\\[(\\d+)])?$")

    /** Resolves simple object paths and zero-based array indexes from a JSON tree. */
    fun get(root: Any?, path: String): Any? {
        if (path.isBlank() || path == "-") return null
        var current: Any? = root
        path.split('.').forEach { segment ->
            val match = segmentPattern.matchEntire(segment) ?: return null
            current = when (current) {
                is JSONObject -> current.opt(match.groupValues[1]).takeUnless { it == JSONObject.NULL }
                is JSONArray -> current.opt(match.groupValues[1].toIntOrNull() ?: return null)
                else -> return null
            }
            match.groupValues[2].takeIf { it.isNotBlank() }?.let { index ->
                val array = current as? JSONArray ?: return null
                current = array.opt(index.toInt())
            }
        }
        return current
    }

    fun text(root: Any?, path: String): String? = get(root, path)?.toString()?.takeUnless { it == "null" }
}
