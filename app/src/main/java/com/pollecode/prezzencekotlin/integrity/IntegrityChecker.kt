package com.pollecode.prezzencekotlin.integrity

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.resume

/**
 * Wraps the Play Integrity Standard API. All calls are best-effort and advisory:
 * any failure (no Play services, sandbox, throttling) resolves to null instead of
 * throwing, so it can never block a legitimate user.
 */
class IntegrityChecker(
    context: Context,
    private val cloudProjectNumber: Long,
) {
    private val appContext = context.applicationContext
    private val manager = IntegrityManagerFactory.createStandard(appContext)

    @Volatile
    private var tokenProvider: StandardIntegrityTokenProvider? = null

    /** Requests a Play Integrity token bound to [payload], or null if unavailable. */
    suspend fun requestToken(payload: String): String? = withContext(Dispatchers.IO) {
        val provider = ensureProvider() ?: return@withContext null
        val requestHash = sha256(payload)
        runCatching {
            suspendCancellableCoroutine<String?> { cont ->
                provider
                    .request(
                        StandardIntegrityTokenRequest.builder()
                            .setRequestHash(requestHash)
                            .build()
                    )
                    .addOnSuccessListener { token -> cont.resume(token.token()) }
                    .addOnFailureListener { _ -> cont.resume(null) }
            }
        }.getOrNull()
    }

    private suspend fun ensureProvider(): StandardIntegrityTokenProvider? {
        tokenProvider?.let { return it }
        if (cloudProjectNumber <= 0L) return null
        return runCatching {
            suspendCancellableCoroutine<StandardIntegrityTokenProvider?> { cont ->
                manager
                    .prepareIntegrityToken(
                        PrepareIntegrityTokenRequest.builder()
                            .setCloudProjectNumber(cloudProjectNumber)
                            .build()
                    )
                    .addOnSuccessListener { provider ->
                        tokenProvider = provider
                        cont.resume(provider)
                    }
                    .addOnFailureListener { _ -> cont.resume(null) }
            }
        }.getOrNull()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
