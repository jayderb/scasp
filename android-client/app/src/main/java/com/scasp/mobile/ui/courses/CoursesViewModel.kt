package com.scasp.mobile.ui.courses

import androidx.lifecycle.SavedStateHandle
import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.model.Course
import com.scasp.mobile.ui.common.ListViewModel

/** The search text is kept in SavedStateHandle, so it also survives process death, not only rotation. */
class CoursesViewModel(private val repo: CampusRepository, private val saved: SavedStateHandle) : ListViewModel<Course>() {
    var query: String
        get() = saved["query"] ?: ""
        private set(v) { saved["query"] = v }

    val canRegister: Boolean get() = repo.currentUser?.staff == false

    override suspend fun fetch() = repo.courses(query)

    fun search(text: String) { query = text.trim().take(50); refresh() }
    fun register(code: String) = runAction({ repo.register(code) })
}
