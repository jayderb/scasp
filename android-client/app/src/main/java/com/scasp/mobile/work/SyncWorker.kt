package com.scasp.mobile.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scasp.mobile.R
import com.scasp.mobile.ScaspApp
import com.scasp.mobile.data.local.LocalCache
import com.scasp.mobile.data.model.Origin
import com.scasp.mobile.data.remote.JsonParser
import com.scasp.mobile.data.ScaspRepository
import com.scasp.mobile.util.Promotions
import java.util.concurrent.TimeUnit

/**
 * Background sync (WorkManager, every 15 minutes, only with a network connection). Refreshes the cached course
 * list and the student's registrations; if a waiting-list entry became a registration it posts a notification
 * (only if the user allowed notifications). Runs off the main thread (CoroutineWorker on Dispatchers.Default + IO).
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = ScaspApp.from(applicationContext)
        val user = app.repository.currentUser ?: return Result.success()        // signed out: nothing to sync
        if (user.demo) return Result.success()
        val key = LocalCache.userKey(user.userId, ScaspRepository.REGISTRATIONS)
        val before = app.cache.get(key)?.let { JsonParser.registrations(it.json) } ?: emptyList()
        return try {
            app.repository.courses("")
            if (user.staff) return Result.success()
            val after = app.repository.registrations()
            if (after.origin == Origin.LIVE) Promotions.newlyRegistered(before, after.data).forEach { notifyPromoted(it) }
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.retry()
        }
    }

    private fun notifyPromoted(courseCode: String) {
        val ctx = applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val n = NotificationCompat.Builder(ctx, ScaspApp.SYNC_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_scasp)
            .setContentTitle(ctx.getString(R.string.promoted_title))
            .setContentText(ctx.getString(R.string.promoted_text, courseCode))
            .setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(courseCode.hashCode(), n)
    }

    companion object {
        private const val NAME = "scasp-sync"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
        fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(NAME) }
    }
}
