package com.scasp.mobile.ui

import androidx.lifecycle.SavedStateHandle
import com.scasp.mobile.FakeRepository
import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.remote.ApiException
import com.scasp.mobile.data.remote.OfflineException
import com.scasp.mobile.ui.campus.CampusViewModel
import com.scasp.mobile.ui.courses.CoursesViewModel
import com.scasp.mobile.ui.login.LoginViewModel
import com.scasp.mobile.ui.registrations.RegistrationsViewModel
import com.scasp.mobile.ui.requests.RequestsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun coursesLoadOnceAndKeepStateForRotation() = runTest(dispatcher) {
        val vm = CoursesViewModel(repo, SavedStateHandle())
        vm.loadIfNeeded(); assertTrue(vm.state.value.loading)
        advanceUntilIdle()
        vm.loadIfNeeded(); advanceUntilIdle()                 // a recreated Activity calls this again
        assertEquals(1, repo.calls.count { it.startsWith("courses") })
        assertEquals(2, vm.state.value.items.size); assertEquals(Origin.LIVE, vm.state.value.origin); assertFalse(vm.state.value.loading)
    }

    @Test fun searchTextIsSavedInSavedStateHandle() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        CoursesViewModel(repo, saved).search("  java ")
        advanceUntilIdle()
        val restored = CoursesViewModel(repo, saved)          // e.g. after process death
        assertEquals("java", restored.query)
        assertTrue(repo.calls.contains("courses:java"))
    }

    @Test fun emptyDataShowsEmptyState() = runTest(dispatcher) {
        repo.registrations = emptyList()
        val vm = RegistrationsViewModel(repo); vm.refresh(); advanceUntilIdle()
        assertTrue(vm.state.value.empty); assertNull(vm.state.value.error)
    }

    @Test fun offlineErrorIsShownAndSessionExpiryIsFlagged() = runTest(dispatcher) {
        repo.failWith = OfflineException("Cannot reach the SCASP server")
        val vm = CoursesViewModel(repo, SavedStateHandle()); vm.refresh(); advanceUntilIdle()
        assertEquals("Cannot reach the SCASP server", vm.state.value.error); assertFalse(vm.state.value.sessionExpired)
        assertFalse("an error is not an empty list", vm.state.value.empty)
        repo.failWith = ApiException(401, "expired"); vm.refresh(); advanceUntilIdle()
        assertTrue(vm.state.value.sessionExpired)
    }

    @Test fun cachedOriginIsExposedForTheBanner() = runTest(dispatcher) {
        repo.origin = Origin.CACHE
        val vm = CampusViewModel(repo); vm.refresh(); advanceUntilIdle()
        assertEquals(Origin.CACHE, vm.state.value.origin); assertEquals(1_000L, vm.state.value.fetchedAtMillis)
    }

    @Test fun registerShowsMessageAndReloads() = runTest(dispatcher) {
        val vm = CoursesViewModel(repo, SavedStateHandle()); vm.register("DB302"); advanceUntilIdle()
        assertEquals("DB302 registered", vm.state.value.message)
        assertEquals(listOf("register:DB302", "courses:"), repo.calls)
        vm.messageShown(); assertNull(vm.state.value.message)
    }

    @Test fun requestValidationHappensBeforeAnyNetworkCall() = runTest(dispatcher) {
        val vm = RequestsViewModel(repo)
        var cleared = false
        vm.submit("TRANSCRIPT", "x", 3) { cleared = true }; advanceUntilIdle()
        assertEquals("Description must be 5-1000 characters", vm.state.value.error)
        assertTrue(repo.calls.isEmpty())
        vm.submit("TRANSCRIPT", "Official transcript", 3) { cleared = true }; advanceUntilIdle()
        assertTrue(cleared); assertEquals("Request #1 submitted.", vm.state.value.message)
        assertNull(RequestsViewModel.validate("OTHER", "Valid text", 5)); assertNotNull(RequestsViewModel.validate("NOPE", "Valid text", 1))
        assertNotNull(RequestsViewModel.validate("OTHER", "Valid text", 6))
    }

    @Test fun loginValidatesThenSignsIn() = runTest(dispatcher) {
        repo.user = null
        val vm = LoginViewModel(repo)
        var applied: String? = null
        vm.login("not a url", "STU001", "student123") { applied = it }
        assertTrue(vm.state.value.error!!.contains("server address")); assertNull(applied)
        vm.login("http://10.0.2.2:8080/scasp", "STU001", "") { applied = it }
        assertEquals("Enter your password", vm.state.value.error)
        vm.login("http://10.0.2.2:8080/scasp", "STU001", "student123") { applied = it }; advanceUntilIdle()
        assertEquals("http://10.0.2.2:8080/scasp", applied); assertEquals("STU001", vm.state.value.user!!.userId)
    }

    @Test fun locationTextNamesTheNearestService() = runTest(dispatcher) {
        val vm = CampusViewModel(repo); vm.refresh(); advanceUntilIdle()
        vm.onLocation(-15.4181, 28.2819, 12f)
        assertTrue(vm.location.value!!, vm.location.value!!.contains("Nearest service: Campus Clinic"))
        vm.onPermissionDenied(permanently = true)
        assertTrue(vm.location.value!!.contains("denied")); assertTrue(vm.permanentlyDenied.value)
    }
}
