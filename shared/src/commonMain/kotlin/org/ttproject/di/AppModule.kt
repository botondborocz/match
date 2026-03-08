package org.ttproject.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.ttproject.repository.AuthRepository
import org.ttproject.repository.AuthRepositoryImpl
import org.ttproject.repository.MatchRepository
import org.ttproject.repository.MatchRepositoryImpl
import org.ttproject.repository.UserRepository
import org.ttproject.repository.UserRepositoryImpl
import org.ttproject.viewmodel.LoginViewModel
import org.ttproject.viewmodel.MatchViewModel
import org.ttproject.viewmodel.ProfileViewModel

expect val platformModule: Module

val appModule = module {
    // Loads the Android or iOS specific dependencies!
    includes(platformModule)

    single { createHttpClient() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<MatchRepository> { MatchRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { MatchViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}