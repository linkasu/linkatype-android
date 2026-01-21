package ru.ibakaidov.distypepro.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.ibakaidov.distypepro.shared.SharedSdk
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.utils.Tts
import ru.ibakaidov.distypepro.utils.TtsHolder
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedSdk(@ApplicationContext context: Context): SharedSdk {
        return SharedSdkProvider.get(context)
    }

    @Provides
    @Singleton
    fun provideTts(@ApplicationContext context: Context): Tts {
        return TtsHolder.get(context)
    }
}
