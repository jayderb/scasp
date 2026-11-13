package com.scasp.mobile

import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.data.model.Course
import com.scasp.mobile.data.model.Loaded
import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.model.RegistrationItem
import com.scasp.mobile.data.model.ServiceRequestItem
import com.scasp.mobile.data.model.UserSession

/** Reads a JSON response captured from the real SCASP server (app/src/test/resources/fixtures). */
fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.getResource("/fixtures/$name")) { "missing fixture $name" }.readText()

val STUDENT = UserSession("STU001", "Alex Banda", "STUDENT", false, "csrf-1")

/** Configurable in-memory repository for ViewModel and Robolectric tests. */
class FakeRepository(var user: UserSession? = STUDENT) : CampusRepository {
    var courses: List<Course> = listOf(Course("CSC301", "Advanced Java", 2, 1, 0), Course("HCI308", "Human Computer Interface", 2, 2, 1))
    var registrations: List<RegistrationItem> = listOf(RegistrationItem("CSC301", "REGISTERED"))
    var requests: List<ServiceRequestItem> = emptyList()
    var services: List<CampusServiceItem> = listOf(
        CampusServiceItem("LIB", "Main Library", "Books", -15.4167, 28.2833),
        CampusServiceItem("REG", "Registry", "Records", -15.4170, 28.2840),
        CampusServiceItem("CLINIC", "Campus Clinic", "Health", -15.4180, 28.2820))
    var origin = Origin.LIVE
    var failWith: Exception? = null
    val calls = mutableListOf<String>()

    private fun <T> loaded(name: String, data: T): Loaded<T> { calls += name; failWith?.let { throw it }; return Loaded(data, origin, 1_000L) }
    override val currentUser get() = user
    override suspend fun login(userId: String, password: String): UserSession { calls += "login"; failWith?.let { throw it }; return STUDENT.also { user = it } }
    override suspend fun logout() { calls += "logout"; user = null }
    override suspend fun courses(query: String) = loaded("courses:$query", courses.filter { query.isBlank() || it.code.contains(query, true) || it.title.contains(query, true) })
    override suspend fun registrations() = loaded("registrations", registrations)
    override suspend fun register(courseCode: String): String { calls += "register:$courseCode"; failWith?.let { throw it }; return "$courseCode registered" }
    override suspend fun withdraw(courseCode: String): String { calls += "withdraw:$courseCode"; return "withdrawn" }
    override suspend fun requests() = loaded("requests", requests)
    override suspend fun submitRequest(category: String, description: String, priority: Int): String { calls += "submit:$category"; return "Request #1 submitted." }
    override suspend fun campusServices() = loaded("campus", services)
}
