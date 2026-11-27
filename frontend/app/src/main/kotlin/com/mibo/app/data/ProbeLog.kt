package com.mibo.app.data

import com.mibo.app.App
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * 实况图发布实验的只读探测日志：把 multimedia/multidiscovery 与图片上传 init 的
 * 响应（脱敏后）落盘，供设置「应用相关」一键复制出来分析。
 * 只保留结构与端点：密钥类字段打码，URL 去掉查询串，长字符串截断。
 */
internal object ProbeLog {
    private const val MAX_BYTES = 180_000
    private val lock = Any()

    private fun file(): File = File(App.context.filesDir, "livephoto_probe.txt")

    fun append(section: String, json: JSONObject?) {
        runCatching {
            val body = if (json == null) "<null>" else redact(json).toString(2)
            write("==== $section ====\n$body\n")
        }
    }

    fun appendText(section: String, body: String) {
        runCatching { write("==== $section ====\n$body\n") }
    }

    private fun write(text: String) {
        synchronized(lock) {
            val f = file()
            if (f.exists() && f.length() > MAX_BYTES) f.writeText("")
            f.appendText(text)
        }
    }

    fun read(): String = runCatching {
        file().takeIf { it.exists() }?.readText().orEmpty()
    }.getOrDefault("")

    private fun redactValue(key: String, value: Any?): Any? {
        val k = key.lowercase()
        if (k.contains("sign") || k.contains("token") || k.contains("secret") ||
            k.contains("gsid") || k.contains("cookie") || k.contains("validator") || k == "auth"
        ) {
            return "<redacted>"
        }
        return when (value) {
            is JSONObject -> redact(value)
            is JSONArray -> redact(value)
            is String -> when {
                value.startsWith("http") -> value.substringBefore('?')
                value.length > 100 -> value.take(60) + "…(" + value.length + ")"
                else -> value
            }
            else -> value
        }
    }

    private fun redact(json: JSONObject): JSONObject {
        val out = JSONObject()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            out.put(k, redactValue(k, json.opt(k)) ?: JSONObject.NULL)
        }
        return out
    }

    private fun redact(arr: JSONArray): JSONArray {
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            out.put(redactValue("", arr.opt(i)) ?: JSONObject.NULL)
        }
        return out
    }
}
