package com.centinela.app.guardian

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.centinela.app.InterruptActivity
import kotlinx.coroutines.*

class GuardianService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var checkJob: Job? = null
    private var lastInterruptedApp: String? = null
    private var lastInterruptTime: Long = 0L

    companion object {
        const val CHANNEL_ID = "centinela_guardian"
        const val NOTIFICATION_ID = 1
        const val CHECK_INTERVAL_MS = 60_000L
        const val USAGE_THRESHOLD_MS = 20 * 60 * 1000L
        const val COOLDOWN_MS = 30 * 60 * 1000L
        private val EXCLUDED_APPS = setOf(
            "com.centinela.app",
            "com.android.systemui",
            "com.android.launcher3",
            "com.motorola.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.huawei.android.launcher"
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startWatching()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startWatching() {
        checkJob = scope.launch {
            while (isActive) {
                checkUsage()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun checkUsage() {
        val usageStats = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val windowStart = now - USAGE_THRESHOLD_MS

        val stats = usageStats.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            windowStart,
            now
        )

        val topApp = stats
            ?.filter { it.packageName !in EXCLUDED_APPS }
            ?.filter { it.lastTimeUsed >= windowStart }
            ?.maxByOrNull { it.totalTimeInForeground }
            ?: return

        if (topApp.totalTimeInForeground < USAGE_THRESHOLD_MS) return

        val cooldownExpired = (now - lastInterruptTime) > COOLDOWN_MS
        val isDifferentApp = topApp.packageName != lastInterruptedApp

        if (isDifferentApp || cooldownExpired) {
            lastInterruptedApp = topApp.packageName
            lastInterruptTime = now
            triggerInterrupt(topApp.packageName, topApp.totalTimeInForeground)
        }
    }

    private fun triggerInterrupt(packageName: String, timeMs: Long) {
        val intent = Intent(this, InterruptActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("package_name", packageName)
            putExtra("time_ms", timeMs)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Guardián Activo",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "CENTINELA vigilando en segundo plano"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("CENTINELA")
        .setContentText("Guardián activo")
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setOngoing(true)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        checkJob?.cancel()
        scope.cancel()
    }
}
