package com.machadothi.templateapp.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Where the phone is -- which, during setup, is where the heliostat is. The sun's
 * position depends on it: a degree of latitude is roughly a degree of sun.
 *
 * Uses the platform LocationManager, so no Google Play services dependency.
 */
@Singleton
class PhoneLocation @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @SuppressLint("MissingPermission") // requested by the permission gate
    suspend fun current(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnown = manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        val fresh = lastKnown != null && System.currentTimeMillis() - lastKnown.time < MAX_AGE_MS
        if (fresh) return lastKnown

        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return lastKnown
        }
        return withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancel = CancellationSignal()
                continuation.invokeOnCancellation { cancel.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    manager, provider, cancel, ContextCompat.getMainExecutor(context),
                ) { location -> if (continuation.isActive) continuation.resume(location) }
            }
        } ?: lastKnown
    }

    private companion object {
        const val MAX_AGE_MS = 30 * 60 * 1000L
        const val FIX_TIMEOUT_MS = 10_000L
    }
}
