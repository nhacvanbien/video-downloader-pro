package com.smarttool.videodownloader.feature.splash.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.smarttool.videodownloader.MainActivity
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.navigation.AppRoute
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.core.ui.widget.CustomSeekbarSplash
import com.smarttool.videodownloader.core.ui.widget.ProgressCallback
import com.smarttool.videodownloader.core.update.InAppUpdate
import com.smarttool.videodownloader.core.update.InstallUpdatedListener
import com.smarttool.videodownloader.core.withAppLocale
import com.smarttool.videodownloader.feature.library.domain.usecase.PruneMissingFilesUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.model.AppEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel

class SplashActivity : AppCompatActivity() {
    private val splashViewModel: SplashViewModel by koinViewModel()

    private val preferences: AppPreferencesDataSource by inject()
    private val pruneMissingFiles: PruneMissingFilesUseCase by inject()
    private lateinit var inAppUpdate: InAppUpdate

    private var loadingText by mutableStateOf("")

    private val progressView by lazy {
        CustomSeekbarSplash(this).apply {
            onProgress = object : ProgressCallback {
                override fun onProgress(progress: Int) {
                    loadingText = getString(R.string.string_loading, progress.toString())
                }
            }
        }
    }

    /** Valid once [SplashViewModel.awaitLoaded] has returned; defaults to "already seen". */
    private val isStartLanguageShowed get() = splashViewModel.uiState.value.startLanguageShown

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale(preferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                SplashScreen(
                    loadingText = loadingText,
                    progressView = progressView,
                )
            }
        }

        initFlow()
    }

    private fun initFlow() {
        lifecycleScope.launch {
            pruneMissingFiles()
        }

        lifecycleScope.launch {
            splashViewModel.awaitLoaded()
            onReady()
        }

        val remoteConfig = FirebaseRemoteConfig.getInstance()

        inAppUpdate = InAppUpdate(this, remoteConfig.getBoolean("force_update"), object :
            InstallUpdatedListener {
            override fun onUpdateNextAction() {
            }

            override fun onUpdateCancel() {
                finish()
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this::inAppUpdate.isInitialized) {
            inAppUpdate.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Onboarding is no longer a chain of Activities: the splash picks where the graph
     * starts and hands that to [MainActivity], which owns every screen from here on.
     */
    private fun startNextAct() {
        lifecycleScope.launch {
            val startDestination = when (splashViewModel.awaitLoaded().entryPoint) {
                AppEntryPoint.Language -> AppRoute.LANGUAGE
                AppEntryPoint.Intro -> AppRoute.INTRO
                AppEntryPoint.Home -> AppRoute.MAIN
            }

            startActivity(
                MainActivity.newIntent(this@SplashActivity, startDestination)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            finish()
        }
    }

    private fun onReady() {
        if (isStartLanguageShowed) {
            startNextAct()
            return
        }

        progressView.progressFlow
            .filter { it >= 90 }
            .onEach { startNextAct() }
            .launchIn(lifecycleScope)
    }
}
