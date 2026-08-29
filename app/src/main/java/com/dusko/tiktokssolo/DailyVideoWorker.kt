package com.dusko.tiktokssolo

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime

class DailyVideoWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("tiktok_solo", Context.MODE_PRIVATE)
        val uris = prefs.getString("media_uris", "")!!
            .split("\n")
            .filter { it.isNotBlank() }
            .map(Uri::parse)

        if (uris.isEmpty()) return Result.failure()

        val duration = prefs.getString("duration", "60")?.toIntOrNull()?.coerceIn(15, 180) ?: 60
        val slot = inputData.getInt("slot", 0)
        val hour = inputData.getLong("hour", 7L)

        MediaMontage(applicationContext).export(uris, duration) { result ->
            result.onSuccess { file ->
                prefs.edit().putString("last_auto_file", file.absolutePath).putInt("last_auto_slot", slot).apply()
            }
        }

        // Re-arm this slot for the next day. WorkManager handles device/network constraints.
        val next = DailyVideoWorker.delayUntilHour(hour, tomorrow = true)
        val request = androidx.work.OneTimeWorkRequestBuilder<DailyVideoWorker>()
            .setInputData(androidx.work.workDataOf("slot" to slot, "hour" to hour))
            .setInitialDelay(next, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "daily_tiktok_solo_$slot",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
        return Result.success()
    }

    companion object {
        fun delayUntilHour(hour: Long, tomorrow: Boolean = false): Long {
            val now = ZonedDateTime.now()
            var target = now.withHour(hour.toInt()).withMinute(0).withSecond(0).withNano(0)
            if (tomorrow || !target.isAfter(now)) target = target.plusDays(1)
            return Duration.between(now, target).toMillis().coerceAtLeast(0L)
        }
    }
}
