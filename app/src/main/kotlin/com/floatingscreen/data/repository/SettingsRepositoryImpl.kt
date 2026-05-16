package com.floatingscreen.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.floatingscreen.domain.model.*
import com.floatingscreen.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val RECORDING_QUALITY = stringPreferencesKey("recording_quality")
        val FRAME_RATE = intPreferencesKey("frame_rate")
        val AUDIO_SOURCE = stringPreferencesKey("audio_source")
        val FLOATING_OPACITY = floatPreferencesKey("floating_opacity")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val AUTO_START_FLOATING = booleanPreferencesKey("auto_start_floating")
        val SHOW_COUNTDOWN = booleanPreferencesKey("show_countdown")
        val VIBRATE_FEEDBACK = booleanPreferencesKey("vibrate_feedback")
        val SAVE_LOCATION = stringPreferencesKey("save_location")
    }

    override fun getSettings(): Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading settings")
                emit(emptyPreferences())
            } else throw exception
        }
        .map { prefs ->
            AppSettings(
                recordingQuality = RecordingQuality.fromLabel(
                    prefs[Keys.RECORDING_QUALITY] ?: RecordingQuality.FHD.label
                ),
                frameRate = FrameRate.fromFps(prefs[Keys.FRAME_RATE] ?: 30),
                audioSource = AudioSource.fromLabel(
                    prefs[Keys.AUDIO_SOURCE] ?: AudioSource.MICROPHONE.label
                ),
                floatingButtonOpacity = prefs[Keys.FLOATING_OPACITY] ?: 0.85f,
                isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
                autoStartFloating = prefs[Keys.AUTO_START_FLOATING] ?: false,
                showCountdownTimer = prefs[Keys.SHOW_COUNTDOWN] ?: true,
                vibrateFeedback = prefs[Keys.VIBRATE_FEEDBACK] ?: true,
                saveLocation = SaveLocation.fromLabel(
                    prefs[Keys.SAVE_LOCATION] ?: SaveLocation.DEFAULT.label
                )
            )
        }

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RECORDING_QUALITY] = settings.recordingQuality.label
            prefs[Keys.FRAME_RATE] = settings.frameRate.fps
            prefs[Keys.AUDIO_SOURCE] = settings.audioSource.label
            prefs[Keys.FLOATING_OPACITY] = settings.floatingButtonOpacity
            prefs[Keys.IS_DARK_THEME] = settings.isDarkTheme
            prefs[Keys.AUTO_START_FLOATING] = settings.autoStartFloating
            prefs[Keys.SHOW_COUNTDOWN] = settings.showCountdownTimer
            prefs[Keys.VIBRATE_FEEDBACK] = settings.vibrateFeedback
            prefs[Keys.SAVE_LOCATION] = settings.saveLocation.label
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
