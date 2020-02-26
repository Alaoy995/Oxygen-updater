package com.arjanvlek.oxygenupdater

import androidx.preference.PreferenceManager
import com.arjanvlek.oxygenupdater.apis.ServerApi
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.createNetworkClient
import com.arjanvlek.oxygenupdater.viewmodels.MainViewModel
import com.arjanvlek.oxygenupdater.viewmodels.NewsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

private val retrofitModule = module {
    single { createNetworkClient(BuildConfig.SERVER_BASE_URL) }
}

private val networkModule = module {
    single { get<Retrofit>().create(ServerApi::class.java) }
}

private val repositoryModule = module {
    single { ServerRepository(get(), get()) }
}

private val viewModelModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { NewsViewModel(get()) }
}

// private val fragmentModule = module {
//     factory { MainFragment() }
//     factory { ErrorFragment() }
// }

private val preferencesModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single { SettingsManager(androidContext()) }
}

val allModules = listOf(
    retrofitModule,
    networkModule,
    repositoryModule,
    viewModelModule,
    // fragmentModule,
    preferencesModule
)
