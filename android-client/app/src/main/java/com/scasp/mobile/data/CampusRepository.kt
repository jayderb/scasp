package com.scasp.mobile.data

import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.data.model.Course
import com.scasp.mobile.data.model.Loaded
import com.scasp.mobile.data.model.RegistrationItem
import com.scasp.mobile.data.model.ServiceRequestItem
import com.scasp.mobile.data.model.UserSession

/** What the screens need. Implemented by [ScaspRepository]; faked in ViewModel tests. */
interface CampusRepository {
    val currentUser: UserSession?
    suspend fun login(userId: String, password: String): UserSession
    suspend fun logout()
    suspend fun courses(query: String): Loaded<List<Course>>
    suspend fun registrations(): Loaded<List<RegistrationItem>>
    suspend fun register(courseCode: String): String
    suspend fun withdraw(courseCode: String): String
    suspend fun requests(): Loaded<List<ServiceRequestItem>>
    suspend fun submitRequest(category: String, description: String, priority: Int): String
    suspend fun campusServices(): Loaded<List<CampusServiceItem>>
}
