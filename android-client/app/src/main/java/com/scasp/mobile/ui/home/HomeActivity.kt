package com.scasp.mobile.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.scasp.mobile.R
import com.scasp.mobile.databinding.ActivityHomeBinding
import com.scasp.mobile.ui.campus.CampusServicesActivity
import com.scasp.mobile.ui.common.BaseActivity
import com.scasp.mobile.ui.courses.CoursesActivity
import com.scasp.mobile.ui.registrations.RegistrationsActivity
import com.scasp.mobile.ui.requests.RequestsActivity
import com.scasp.mobile.work.SyncWorker
import kotlinx.coroutines.launch

/** Dashboard. Every button uses an explicit intent to a screen of this app. */
class HomeActivity : BaseActivity() {
    private lateinit var b: ActivityHomeBinding
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sync works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = app.repository.currentUser ?: run { goToLogin(null); return }
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.welcome.text = getString(R.string.welcome, user.name, user.userId, user.role)
        b.demoNotice.visibility = if (user.demo) View.VISIBLE else View.GONE
        b.server.text = getString(R.string.connected_to, app.settings.baseUrl)
        b.openCourses.setOnClickListener { startActivity(Intent(this, CoursesActivity::class.java)) }
        b.openRegistrations.isEnabled = !user.staff
        b.openRegistrations.text = getString(if (user.staff) R.string.registrations_students_only else R.string.my_registrations)
        b.openRegistrations.setOnClickListener { startActivity(Intent(this, RegistrationsActivity::class.java)) }
        b.openRequests.text = getString(if (user.staff) R.string.request_queue else R.string.service_requests)
        b.openRequests.setOnClickListener { startActivity(Intent(this, RequestsActivity::class.java)) }
        b.openCampus.setOnClickListener { startActivity(Intent(this, CampusServicesActivity::class.java)) }

        b.backgroundSync.isChecked = app.settings.backgroundSync
        b.backgroundSync.setOnCheckedChangeListener { _, on ->
            app.settings.backgroundSync = on
            if (on) {
                SyncWorker.schedule(this)
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)   // asked only when the user turns sync on
            } else SyncWorker.cancel(this)
        }
        b.signOut.setOnClickListener {
            lifecycleScope.launch {
                SyncWorker.cancel(this@HomeActivity); app.settings.backgroundSync = false
                app.repository.logout()
                goToLogin(getString(R.string.signed_out))
            }
        }
    }
}
