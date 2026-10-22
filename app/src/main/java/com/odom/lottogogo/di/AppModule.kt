package com.odom.lottogogo.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.odom.lottogogo.data.local.DataStoreSyncStatusStore
import com.odom.lottogogo.data.local.SyncStatusStore
import com.odom.lottogogo.domain.DrawSchedule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindSyncStatusStore(impl: DataStoreSyncStatusStore): SyncStatusStore

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.system(DrawSchedule.ZONE)

        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("app_prefs") }
    }
}
