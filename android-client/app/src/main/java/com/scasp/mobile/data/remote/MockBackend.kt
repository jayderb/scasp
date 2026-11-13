package com.scasp.mobile.data.remote

import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.data.model.Course
import com.scasp.mobile.data.model.RegistrationItem
import com.scasp.mobile.data.model.ServiceRequestItem
import com.scasp.mobile.data.model.UserSession

/**
 * Controlled mock of the SCASP server used ONLY when the real server cannot be reached and the user allowed
 * demo data (login screen switch). Mirrors database/sample-data.sql. Every screen shows "Demo data" while it is used,
 * and nothing done here is sent to the server.
 */
class MockBackend {
    private val accounts = mapOf(
        "STU001" to Triple("student123", "Alex Banda", "STUDENT"),
        "STU002" to Triple("student123", "Ruth Phiri", "STUDENT"),
        "STAFF01" to Triple("staff123", "Grace Tembo", "STAFF"),
    )
    private val courses = linkedMapOf(
        "CSC301" to Course("CSC301", "Advanced Java", 2, 1, 0),
        "DB302" to Course("DB302", "Advanced Database", 3, 0, 0),
        "HCI308" to Course("HCI308", "Human Computer Interface", 2, 2, 1),
    )
    private val mine = mutableListOf<RegistrationItem>()
    private val requests = mutableListOf<ServiceRequestItem>()

    val campusServices = listOf(
        CampusServiceItem("CLINIC", "Campus Clinic", "Basic health services (illustrative coordinates)", -15.4180, 28.2820),
        CampusServiceItem("LIB", "Main Library", "Books and study space (illustrative coordinates)", -15.4167, 28.2833),
        CampusServiceItem("REG", "Registry", "Academic records and registration (illustrative coordinates)", -15.4170, 28.2840),
    )

    @Synchronized fun login(userId: String, password: String): UserSession {
        val id = userId.trim().uppercase()
        val acc = accounts[id] ?: throw ApiException(401, "Invalid user id or password.")
        if (acc.first != password) throw ApiException(401, "Invalid user id or password.")
        return UserSession(id, acc.second, acc.third, acc.third != "STUDENT", "demo", demo = true)
    }

    @Synchronized fun courses(query: String): List<Course> =
        courses.values.filter { query.isBlank() || it.code.contains(query, true) || it.title.contains(query, true) }

    @Synchronized fun registrations(): List<RegistrationItem> = mine.toList()

    @Synchronized fun register(code: String): String {
        val c = courses[code] ?: throw ApiException(404, "Unknown course $code")
        if (mine.any { it.courseCode == code }) throw ApiException(409, "Duplicate registration")
        return if (c.full) {
            courses[code] = c.copy(waiting = c.waiting + 1)
            mine += RegistrationItem(code, "WAITLISTED", c.waiting + 1)
            "$code is full: you were added to the waiting list (demo)."
        } else {
            courses[code] = c.copy(enrolled = c.enrolled + 1)
            mine += RegistrationItem(code, "REGISTERED")
            "You are registered for $code (demo)."
        }
    }

    @Synchronized fun withdraw(code: String): String {
        val r = mine.firstOrNull { it.courseCode == code } ?: throw ApiException(404, "You are not registered for $code")
        mine.remove(r)
        courses[code]?.let { c -> courses[code] = if (r.status == "REGISTERED") c.copy(enrolled = c.enrolled - 1) else c.copy(waiting = c.waiting - 1) }
        return "Withdrawn from $code (demo)."
    }

    @Synchronized fun requests(): List<ServiceRequestItem> = requests.toList()

    @Synchronized fun submit(category: String, description: String, priority: Int): String {
        val id = (requests.size + 1).toString()
        requests.add(0, ServiceRequestItem(id, category, description, priority, "OPEN", null))
        return "Request #$id saved in demo mode (not sent to the server)."
    }
}
