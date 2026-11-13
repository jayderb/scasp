package com.scasp.mobile.ui.requests

import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.model.SERVICE_REQUEST_CATEGORIES
import com.scasp.mobile.data.model.ServiceRequestItem
import com.scasp.mobile.ui.common.ListViewModel
import kotlinx.coroutines.flow.update

class RequestsViewModel(private val repo: CampusRepository) : ListViewModel<ServiceRequestItem>() {
    val isStaff: Boolean get() = repo.currentUser?.staff == true
    override suspend fun fetch() = repo.requests()

    /** Client-side check mirrors the server rules (server validates again). */
    fun submit(category: String, description: String, priority: Int, onSubmitted: () -> Unit) {
        val problem = validate(category, description, priority)
        if (problem != null) { _state.update { it.copy(error = problem) }; return }
        runAction({ repo.submitRequest(category, description.trim(), priority) }, onSubmitted)
    }

    companion object {
        fun validate(category: String, description: String, priority: Int): String? = when {
            category !in SERVICE_REQUEST_CATEGORIES -> "Choose a category"
            description.trim().length !in 5..1000 -> "Description must be 5-1000 characters"
            priority !in 1..5 -> "Priority must be 1-5"
            else -> null
        }
    }
}
