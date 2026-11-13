package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
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

    fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context.applicationContext).isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp check failed: ${e.message}")
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

        val clientId = try {
            @Suppress("DiscouragedApi")
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else throw IllegalStateException("default_web_client_id not found")
        } catch (e: Exception) {
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
            onAuthError("Google Sign-In is not configured in this build. Use Phone & PIN login or add google-services.json.")
            return
        }

        val clientId = try {
            @Suppress("DiscouragedApi")
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else throw IllegalStateException("default_web_client_id not found")
        } catch (e: Exception) {
            onAuthError("Google Sign-In configuration missing: google-services.json is required")
            return
        }

        val activity = context as? Activity
        if (activity == null) {
            onAuthError("Context is not an Activity")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed")
            }
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
