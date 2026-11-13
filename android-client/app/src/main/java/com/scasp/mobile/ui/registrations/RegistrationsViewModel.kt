package com.scasp.mobile.ui.registrations

import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.model.RegistrationItem
import com.scasp.mobile.ui.common.ListViewModel

class RegistrationsViewModel(private val repo: CampusRepository) : ListViewModel<RegistrationItem>() {
    override suspend fun fetch() = repo.registrations()
    fun withdraw(code: String) = runAction({ repo.withdraw(code) })
}
