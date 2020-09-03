package com.github.droibit.datastore.sample.data

import android.content.Context
import android.util.Log
import androidx.datastore.DataStore
import androidx.datastore.preferences.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val USER_PREFERENCES_NAME = "user_preferences"

enum class SortOrder {
    NONE,
    BY_DEADLINE,
    BY_PRIORITY,
    BY_DEADLINE_AND_PRIORITY
}

data class UserPreferences(
    val showCompleted: Boolean,
    val sortOrder: SortOrder
)

class UserPreferencesRepository(context: Context) {

    private val dataStore: DataStore<Preferences> = context.createDataStore(
        name = USER_PREFERENCES_NAME,
        migrations = listOf(SharedPreferencesMigration(context, USER_PREFERENCES_NAME))
    )

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKey.SORT_ORDER] ?: SortOrder.NONE.name
            )
            val showCompleted = preferences[PreferencesKey.SHOW_COMPLETED] ?: false
            UserPreferences(showCompleted, sortOrder)
        }

    suspend fun enableSortByDeadline(enable: Boolean) {
        dataStore.edit { preferences ->
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKey.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder = if (enable) {
                if (currentOrder == SortOrder.BY_PRIORITY) {
                    SortOrder.BY_DEADLINE_AND_PRIORITY
                } else {
                    SortOrder.BY_DEADLINE
                }
            } else {
                if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                    SortOrder.BY_PRIORITY
                } else {
                    SortOrder.NONE
                }
            }
            preferences[PreferencesKey.SORT_ORDER] = newSortOrder.name
        }
    }

    suspend fun enableSortByPriority(enable: Boolean) {
        dataStore.edit { preferences ->
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKey.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder = if (enable) {
                    if (currentOrder == SortOrder.BY_DEADLINE) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_PRIORITY
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_DEADLINE
                    } else {
                        SortOrder.NONE
                    }
                }

            preferences[PreferencesKey.SORT_ORDER] = newSortOrder.name
        }
    }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.SHOW_COMPLETED] = showCompleted
        }
    }

    private object PreferencesKey {
        val SORT_ORDER = preferencesKey<String>("sort_order")
        val SHOW_COMPLETED = preferencesKey<Boolean>("show_completed")
    }

    companion object {
        private val TAG: String = "UserPreferencesRepo"
    }
}