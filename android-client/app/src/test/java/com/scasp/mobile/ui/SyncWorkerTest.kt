package com.scasp.mobile.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.scasp.mobile.FakeRepository
import com.scasp.mobile.ScaspApp
import com.scasp.mobile.data.remote.OfflineException
import com.scasp.mobile.work.SyncWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SyncWorkerTest {
    private val app: ScaspApp get() = ApplicationProvider.getApplicationContext()

    @Test fun refreshesWhenSignedInAndSkipsWhenSignedOut() = runBlocking {
        val repo = FakeRepository(); app.repositoryOverride = repo
        val w = TestListenableWorkerBuilder<SyncWorker>(app).build()
        assertEquals(ListenableWorker.Result.success(), w.doWork())
        assertTrue(repo.calls.containsAll(listOf("courses:", "registrations")))
        repo.user = null; repo.calls.clear()
        assertEquals(ListenableWorker.Result.success(), w.doWork())
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun retriesLaterWhenTheServerIsUnreachable() = runBlocking {
        val repo = FakeRepository().apply { failWith = OfflineException("offline") }; app.repositoryOverride = repo
        assertEquals(ListenableWorker.Result.retry(), TestListenableWorkerBuilder<SyncWorker>(app).build().doWork())
    }
}
