package com.github.droibit.datastore.proto.sample.data

import android.content.Context
import android.util.Log
import androidx.datastore.DataStore
import androidx.datastore.createDataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import com.github.droibit.datastore.proto.sample.UserPreferences
import com.github.droibit.datastore.proto.sample.UserPreferences.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val DATA_STORE_FILE_NAME = "user_prefs.pb"
private const val SORT_ORDER_KEY = "sort_order"

class UserPreferencesRepository(context: Context) {

    private val sharedPrefsMigration = SharedPreferencesMigration<UserPreferences>(
        context,
        USER_PREFERENCES_NAME
    ) { sharedPrefs, currentData ->
        if (currentData.sortOrder == SortOrder.UNSPECIFIED) {
            currentData.toBuilder().setSortOrder(
                SortOrder.valueOf(
                    requireNotNull(sharedPrefs.getString(SORT_ORDER_KEY, SortOrder.NONE.name))
                )
            ).build()
        } else {
            currentData
        }
    }

    private val userPreferencesStore: DataStore<UserPreferences> = context.createDataStore(
        fileName = DATA_STORE_FILE_NAME,
        serializer = UserPreferencesSerializer,
        migrations = listOf(sharedPrefsMigration)
    )

    val userPreferencesFlow: Flow<UserPreferences> = userPreferencesStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading sort order preferences.", exception)
                emit(UserPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun enableSortByDeadline(enable: Boolean) {
        userPreferencesStore.updateData { currentPreferences ->
            val currentOrder = currentPreferences.sortOrder
            val newSortOrder =
                if (enable) {
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
            currentPreferences.toBuilder().setSortOrder(newSortOrder).build()
        }
    }

    suspend fun enableSortByPriority(enable: Boolean) {
        userPreferencesStore.updateData { currentPreferences ->
            val currentOrder = currentPreferences.sortOrder
            val newSortOrder =
                if (enable) {
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
            currentPreferences.toBuilder().setSortOrder(newSortOrder).build()
        }
    }

    suspend fun updateShowCompleted(completed: Boolean) {
        userPreferencesStore.updateData { currentPreferences ->
            currentPreferences.toBuilder().setShowCompleted(completed).build()
        }
    }

    companion object {
        private const val TAG: String = "UserPreferencesRepo"
    }
}