package com.example.ocrjizhang.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.sessionDataStore by preferencesDataStore(name = "session_preferences")

data class SessionSnapshot(
    val token: String = "",
    val userId: Long? = null,
    val username: String = "",
    val nickname: String = "",
)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val token = stringPreferencesKey("token")
        val userId = longPreferencesKey("user_id")
        val username = stringPreferencesKey("username")
        val nickname = stringPreferencesKey("nickname")
    }

    val sessionFlow: Flow<SessionSnapshot> = context.sessionDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map(::toSnapshot)

    suspend fun saveSession(snapshot: SessionSnapshot) {
        context.sessionDataStore.edit { preferences ->
            preferences[Keys.token] = snapshot.token
            snapshot.userId?.let { preferences[Keys.userId] = it }
            preferences[Keys.username] = snapshot.username
            preferences[Keys.nickname] = snapshot.nickname
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }

    fun getTokenBlocking(): String = runBlocking {
        sessionFlow.first().token
    }

    private fun toSnapshot(preferences: Preferences): SessionSnapshot =
        SessionSnapshot(
            token = preferences[Keys.token].orEmpty(),
            userId = preferences[Keys.userId],
            username = preferences[Keys.username].orEmpty(),
            nickname = preferences[Keys.nickname].orEmpty(),
        )
}
