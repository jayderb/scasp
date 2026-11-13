package com.scasp.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.scasp.mobile.data.CampusRepository
import com.scasp.mobile.data.ScaspRepository
import com.scasp.mobile.data.local.LocalCache
import com.scasp.mobile.data.local.SessionStore
import com.scasp.mobile.data.local.Settings
import com.scasp.mobile.data.local.SharedPrefsStore
import com.scasp.mobile.data.remote.HttpScaspApi

/** Application object = simple service locator (one repository for all screens and the sync worker). */
class ScaspApp : Application() {
    lateinit var settings: Settings; private set
    lateinit var session: SessionStore; private set
    lateinit var cache: LocalCache; private set
    var repositoryOverride: CampusRepository? = null       // tests can swap in a fake
    private lateinit var realRepository: CampusRepository
    val repository: CampusRepository get() = repositoryOverride ?: realRepository

    override fun onCreate() {
        super.onCreate()
        val prefs = SharedPrefsStore(getSharedPreferences("scasp", Context.MODE_PRIVATE))
        settings = Settings(prefs, BuildConfig.DEFAULT_BASE_URL)
        session = SessionStore(prefs)
        cache = LocalCache(SharedPrefsStore(getSharedPreferences("scasp_cache", Context.MODE_PRIVATE)))
        realRepository = ScaspRepository(HttpScaspApi({ settings.baseUrl }, session), cache, session, settings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(SYNC_CHANNEL, getString(R.string.sync_channel_name), NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        const val SYNC_CHANNEL = "scasp-sync"
        fun from(context: Context) = context.applicationContext as ScaspApp
    }
}
