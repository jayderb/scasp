package com.scasp.mobile.data.remote

import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.data.model.Course
import com.scasp.mobile.data.model.RegistrationItem
import com.scasp.mobile.data.model.ServiceRequestItem
import com.scasp.mobile.data.model.UserSession
import org.json.JSONException
import org.json.JSONObject

/** Parses the SCASP JSON API responses (see docs/architecture/network-protocol.md, "Mobile JSON API"). */
object JsonParser {
    fun user(json: String): UserSession = obj(json).let {
        UserSession(it.getString("userId"), it.getString("name"), it.getString("role"),
            it.optBoolean("staff", false), it.optString("csrfToken", ""))
    }

    fun courses(json: String): List<Course> = obj(json).getJSONArray("courses").let { a ->
        (0 until a.length()).map { i ->
            val c = a.getJSONObject(i)
            Course(c.getString("courseCode"), c.getString("title"), c.getInt("capacity"), c.optInt("enrolled"), c.optInt("waiting"))
        }
    }

    fun registrations(json: String): List<RegistrationItem> {
        val o = obj(json)
        val out = mutableListOf<RegistrationItem>()
        o.optJSONArray("registrations")?.let { a -> for (i in 0 until a.length()) out += RegistrationItem(a.getJSONObject(i).getString("courseCode"), "REGISTERED") }
        o.optJSONArray("waiting")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { out += RegistrationItem(it.getString("courseCode"), "WAITLISTED", it.optInt("position")) }
        }
        return out
    }

    fun requests(json: String): List<ServiceRequestItem> = obj(json).getJSONArray("requests").let { a ->
        (0 until a.length()).map { i ->
            val r = a.getJSONObject(i)
            ServiceRequestItem(r.getString("requestId"), r.getString("category"), r.optString("description"),
                r.optInt("priority", 1), r.getString("status"), r.optNullableString("createdAt"))
        }
    }

    fun campusServices(json: String): List<CampusServiceItem> = obj(json).getJSONArray("services").let { a ->
        (0 until a.length()).map { i ->
            val s = a.getJSONObject(i)
            CampusServiceItem(s.getString("serviceId"), s.getString("name"), s.optString("description"),
                s.getDouble("latitude"), s.getDouble("longitude"), s.optBoolean("illustrative", true))
        }
    }

    /** "message" of an action response, or the fallback. */
    fun message(json: String, fallback: String): String = try { obj(json).optString("message", fallback) } catch (e: ApiException) { fallback }

    /** "error" text of an error response, plus the first field error if present; null if the body is not JSON. */
    fun error(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            val base = o.optNullableString("error") ?: return null
            val fields = o.optJSONObject("fieldErrors")
            if (fields != null && fields.length() > 0) {
                val k = fields.keys().next()
                "$base ($k: ${fields.getString(k)})"
            } else base
        } catch (e: JSONException) { null }
    }

    private fun obj(json: String): JSONObject = try { JSONObject(json) } catch (e: JSONException) {
        throw ApiException(502, "The server sent an unexpected response")
    }
    private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else optString(key, null)
}
