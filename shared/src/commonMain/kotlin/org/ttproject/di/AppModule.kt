package org.ttproject.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.ttproject.repository.AuthRepository
import org.ttproject.repository.AuthRepositoryImpl
import org.ttproject.repository.MatchRepository
import org.ttproject.repository.MatchRepositoryImpl
import org.ttproject.viewmodel.LoginViewModel
import org.ttproject.viewmodel.MatchViewModel

expect val platformModule: Module

val appModule = module {
    // Loads the Android or iOS specific dependencies!
    includes(platformModule)

    single { createHttpClient() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<MatchRepository> { MatchRepositoryImpl(get(), get()) }
    factory { LoginViewModel(get()) }
    factory { MatchViewModel(get()) }
}