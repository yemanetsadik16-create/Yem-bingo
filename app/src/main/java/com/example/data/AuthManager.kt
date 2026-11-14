package com.example.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthManager {

    private const val TAG = "AuthManager"

    @SuppressLint("DiscouragedApi")
    internal fun resolveDefaultWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resId != 0) context.getString(resId).trim() else ""
        } catch (e: Exception) {
            Log.w(TAG, "Unable to resolve default_web_client_id: ${e.message}")
            ""
        }
    }

    fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context.applicationContext).isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp check failed: ${e.message}")
            false
        }
    }

    fun hasCurrentUser(context: Context): Boolean {
        return try {
            isFirebaseInitialized(context) && Firebase.auth.currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (!isFirebaseInitialized(context)) {
            onUnauthenticated()
            return
        }

        val alreadySignedIn = try {
            Firebase.auth.currentUser != null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read Firebase currentUser: ${e.message}")
            false
        }
        if (alreadySignedIn) {
            onAuthSuccess()
            return
        }

        val clientId = resolveDefaultWebClientId(context)
        if (clientId.isBlank()) {
            Log.w(TAG, "default_web_client_id missing or blank; skipping auto sign-in.")
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        if (!isFirebaseInitialized(context)) {
            onAuthError("Google Sign-In requires Firebase configuration (google-services.json).")
            return
        }

        val clientId = resolveDefaultWebClientId(context)
        if (clientId.isBlank()) {
            onAuthError("Google Sign-In requires google-services.json (default_web_client_id is missing).")
            return
        }

        val activity = context as? Activity
        if (activity == null) {
            onAuthError("Unable to open Google Sign-In dialog from this context.")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                completeFirebaseSignIn(result.credential, onAuthSuccess, onAuthError)
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "GetSignInWithGoogleOption returned NoCredentialException; trying Google account picker", e)
                tryFallbackGoogleIdSignIn(
                    activity = activity,
                    credentialManager = credentialManager,
                    clientId = clientId,
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError,
                    onAuthCancelled = onAuthCancelled
                )
            } catch (e: GetCredentialUnsupportedException) {
                Log.w(TAG, "Credential Manager SignInWithGoogle unsupported; trying GoogleIdOption", e)
                tryFallbackGoogleIdSignIn(
                    activity = activity,
                    credentialManager = credentialManager,
                    clientId = clientId,
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError,
                    onAuthCancelled = onAuthCancelled
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
            }
        }
    }

    private suspend fun tryFallbackGoogleIdSignIn(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        try {
            val result = credentialManager.getCredential(activity, fallbackRequest)
            completeFirebaseSignIn(result.credential, onAuthSuccess, onAuthError)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Fallback Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available on device", e)
            onAuthError("No Google account found on this device. Add a Google account in device Settings > Accounts and tap Continue with Google again.")
        } catch (e: Exception) {
            Log.e(TAG, "Fallback Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In unavailable on this device. Ensure Google Play Services is active and a Google account is signed in.")
        }
    }

    private suspend fun completeFirebaseSignIn(
        credential: androidx.credentials.Credential,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            Firebase.auth.signInWithCredential(authCredential).await()
            onAuthSuccess()
        } else {
            onAuthError("Unexpected credential type received.")
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    try {
                        Firebase.auth.signOut()
                    } catch (_: Exception) {}
                    onSignOutComplete()
                }
            }
        }
    }
}
