package com.pixsonlin.qfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixsonlin.qfit.service.RunForegroundService
import com.pixsonlin.qfit.service.RunSessionState
import com.pixsonlin.qfit.ui.AboutScreen
import com.pixsonlin.qfit.ui.HistoryScreen
import com.pixsonlin.qfit.ui.HomeScreen
import com.pixsonlin.qfit.ui.InProgressScreen
import com.pixsonlin.qfit.ui.theme.QFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QFitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QFitNav()
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val IN_PROGRESS = "in_progress"
    const val HISTORY = "history"
    const val ABOUT = "about"
}

@Composable
private fun QFitNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val start = if (RunSessionState.active.value?.finished == false) {
        Routes.IN_PROGRESS
    } else {
        Routes.HOME
    }

    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(280),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(280),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(280),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(280),
            )
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onStarted = {
                    navController.navigate(Routes.IN_PROGRESS) {
                        launchSingleTop = true
                    }
                },
                onStartRun = { intensity, durationMinutes ->
                    RunForegroundService.start(context, intensity, durationMinutes)
                },
            )
        }
        composable(Routes.IN_PROGRESS) {
            InProgressScreen(
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onCancelConfirmed = {
                    RunForegroundService.cancel(context)
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onFinishedNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onBackHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
