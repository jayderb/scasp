package com.scasp.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scasp.mobile.FakeRepository
import com.scasp.mobile.R
import com.scasp.mobile.ScaspApp
import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.remote.OfflineException
import com.scasp.mobile.ui.campus.CampusServicesActivity
import com.scasp.mobile.ui.courses.CoursesActivity
import com.scasp.mobile.ui.home.HomeActivity
import com.scasp.mobile.ui.login.LoginActivity
import com.scasp.mobile.ui.registrations.RegistrationsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Runs the real Activities on the JVM (Robolectric) with a fake repository: no server or emulator needed. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ActivityRobolectricTest {
    private val app: ScaspApp get() = ApplicationProvider.getApplicationContext()
    private lateinit var repo: FakeRepository

    @Before fun setUp() { repo = FakeRepository(); app.repositoryOverride = repo }
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test fun loginWithBadInputShowsErrorThenNavigatesHomeWithAnExplicitIntent() {
        repo.user = null
        ActivityScenario.launch(LoginActivity::class.java).onActivity { a ->
            a.findViewById<EditText>(R.id.userId).setText("STU001")
            a.findViewById<Button>(R.id.signIn).performClick(); idle()
            assertEquals("Enter your password", a.findViewById<TextView>(R.id.error).text.toString())
            a.findViewById<EditText>(R.id.password).setText("student123")
            a.findViewById<Button>(R.id.signIn).performClick(); idle()
            val next = shadowOf(a).nextStartedActivity
            assertEquals(HomeActivity::class.java.name, next.component!!.className)
        }
    }

    @Test fun homeButtonsUseExplicitIntents() {
        ActivityScenario.launch(HomeActivity::class.java).onActivity { a ->
            mapOf(R.id.openCourses to CoursesActivity::class.java, R.id.openRegistrations to RegistrationsActivity::class.java,
                  R.id.openCampus to CampusServicesActivity::class.java).forEach { (id, target) ->
                a.findViewById<Button>(id).performClick()
                assertEquals(target.name, shadowOf(a).nextStartedActivity.component!!.className)
            }
        }
    }

    @Test fun rotationKeepsListAndSearchWithoutReloading() {
        val s = ActivityScenario.launch(CoursesActivity::class.java)
        s.onActivity { a -> idle(); a.findViewById<EditText>(R.id.search).setText("java") }
        s.recreate()                                                         // configuration change (rotation)
        s.onActivity { a ->
            idle()
            assertEquals("java", a.findViewById<EditText>(R.id.search).text.toString())
            assertEquals(2, a.findViewById<RecyclerView>(R.id.recycler).adapter!!.itemCount)
        }
        assertEquals("loaded once, not again after rotation", 1, repo.calls.count { it.startsWith("courses") })
    }

    @Test fun offlineCachedDataShowsTheOfflineBanner() {
        repo.origin = Origin.CACHE
        ActivityScenario.launch(CoursesActivity::class.java).onActivity { a ->
            idle()
            val banner = a.findViewById<TextView>(R.id.banner)
            assertEquals(View.VISIBLE, banner.visibility); assertTrue(banner.text.startsWith("Offline"))
        }
    }

    @Test fun offlineWithoutDataShowsTheErrorNotAnEmptyList() {
        repo.failWith = OfflineException("Cannot reach the SCASP server at http://10.0.2.2:8080/scasp")
        ActivityScenario.launch(RegistrationsActivity::class.java).onActivity { a ->
            idle()
            assertEquals(View.VISIBLE, a.findViewById<TextView>(R.id.error).visibility)
            assertEquals(View.GONE, a.findViewById<TextView>(R.id.empty).visibility)
        }
    }

    @Test fun emptyDataShowsTheEmptyMessage() {
        repo.registrations = emptyList()
        ActivityScenario.launch(RegistrationsActivity::class.java).onActivity { a ->
            idle()
            val empty = a.findViewById<TextView>(R.id.empty)
            assertEquals(View.VISIBLE, empty.visibility); assertTrue(empty.text.contains("no registrations"))
        }
    }

    @Test fun locationPermissionIsRequestedOnlyWhenTheUserTaps() {
        ActivityScenario.launch(CampusServicesActivity::class.java).onActivity { a ->
            idle()
            assertNull("not requested on open", shadowOf(a).lastRequestedPermission)
            a.findViewById<Button>(R.id.showPosition).performClick()
            val req = shadowOf(a).lastRequestedPermission
            assertNotNull(req)
            assertTrue(req.requestedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
            a.onRequestPermissionsResult(req.requestCode, req.requestedPermissions,       // user taps "Don't allow"
                IntArray(req.requestedPermissions.size) { PackageManager.PERMISSION_DENIED }); idle()
            val text = a.findViewById<TextView>(R.id.locationText)
            assertEquals(View.VISIBLE, text.visibility); assertTrue(text.text.contains("denied"))
        }
    }

    @Test fun openInMapFiresTheImplicitGeoIntent() {
        ActivityScenario.launch(CampusServicesActivity::class.java).onActivity { a ->
            idle()
            val rv = a.findViewById<RecyclerView>(R.id.recycler)
            rv.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY))
            rv.layout(0, 0, 1080, 1920)
            assertEquals(3, rv.adapter!!.itemCount)
            rv.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<Button>(R.id.action).performClick()
            val chooser = shadowOf(a).nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            @Suppress("DEPRECATION") val geo = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_VIEW, geo.action)
            assertEquals("geo:-15.4167,28.2833?q=-15.4167,28.2833(Main%20Library)", geo.data.toString())
            assertNull("implicit: no component named", geo.component)
        }
    }
}
