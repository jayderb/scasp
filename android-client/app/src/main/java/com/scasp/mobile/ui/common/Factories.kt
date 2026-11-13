package com.scasp.mobile.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.scasp.mobile.ScaspApp
import com.scasp.mobile.data.CampusRepository

/** Gives ViewModel factories access to the app-wide repository. */
fun CreationExtras.repository(): CampusRepository = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ScaspApp).repository
fun CreationExtras.app(): ScaspApp = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ScaspApp
fun CreationExtras.savedState() = createSavedStateHandle()
