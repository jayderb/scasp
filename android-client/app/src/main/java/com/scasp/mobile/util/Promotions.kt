package com.scasp.mobile.util

import com.scasp.mobile.data.model.RegistrationItem

object Promotions {
    /** Courses that were WAITLISTED in [before] and are REGISTERED in [after] (used by the sync notification). */
    fun newlyRegistered(before: List<RegistrationItem>, after: List<RegistrationItem>): List<String> {
        val waiting = before.filter { it.status == "WAITLISTED" }.map { it.courseCode }.toSet()
        return after.filter { it.status == "REGISTERED" && it.courseCode in waiting }.map { it.courseCode }
    }
}
