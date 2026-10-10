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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixsonlin.qfit.data.HistoryRepository
import com.pixsonlin.qfit.service.RunForegroundService
import com.pixsonlin.qfit.service.RunSessionState
import com.pixsonlin.qfit.ui.AboutScreen
import com.pixsonlin.qfit.ui.HistoryScreen
import com.pixsonlin.qfit.ui.HomeScreen
import com.pixsonlin.qfit.ui.InProgressScreen
import com.pixsonlin.qfit.ui.theme.QFitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

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
    val repo = remember(context) { HistoryRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var foregroundGeneration by remember { mutableIntStateOf(0) }

    // Always root at Home. Starting on In-progress (when RunSessionState survived Activity
    // death) produced a back stack with no Home entry; the old popBackStack(HOME) then
    // failed after the run finished and left Cancel disabled.
    fun navigateHomeFromRun() {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.IN_PROGRESS) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToRun() {
        if (navController.currentDestination?.route == Routes.IN_PROGRESS) return
        navController.navigate(Routes.IN_PROGRESS) {
            launchSingleTop = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                foregroundGeneration += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Bypass collectAsStateWithLifecycle (paused while STOPPED): when the service marks
    // finished during screen-off, still leave In-progress as soon as we hear the Flow.
    // Do not clear the in-memory row until Room has no RUNNING entry — otherwise a cancel
    // that has not finalized yet would look like an orphan and be resumed.
    LaunchedEffect(Unit) {
        RunSessionState.active.collect { active ->
            if (active?.finished != true) return@collect
            if (navController.currentDestination?.route == Routes.IN_PROGRESS) {
                navigateHomeFromRun()
            }
            if (repo.getRunningRun() == null) {
                RunSessionState.setActive(null)
            }
        }
    }

    // Room is authoritative after process death or a screen-off / foreground transition.
    // Reconcile navigation because the in-memory state and restored NavHost back stack can
    // otherwise disagree, leaving In-progress visible with no cancellable run.
    LaunchedEffect(foregroundGeneration) {
        val route = navController.currentBackStackEntryFlow
            .first()
            .destination
            .route
        val running = repo.getRunningRun()
        val active = RunSessionState.active.value
        when {
            // True orphan after process death.
            running != null && active == null -> {
                RunForegroundService.resume(context, running)
                navigateToRun()
            }
            // Activity recreated while the FGS kept RunSessionState; Nav rooted at Home.
            running != null &&
                active?.finished == false &&
                route != Routes.IN_PROGRESS -> {
                navigateToRun()
            }
            route == Routes.IN_PROGRESS &&
                (running == null || active?.finished == true) -> {
                navigateHomeFromRun()
                if (running == null) {
                    RunSessionState.setActive(null)
                }
            }
            // Cancel/finish already left the Run screen; Room caught up afterward.
            active?.finished == true && running == null -> {
                RunSessionState.setActive(null)
            }
        }
        // Finalize can still be in flight right as the screen turns on past end time.
        // Recheck once so we do not stay on In-progress with Cancel already disabled.
        val endMillis = active?.endTimeMillis ?: running?.plannedEndTimeMillis
        if (
            route == Routes.IN_PROGRESS &&
            endMillis != null &&
            System.currentTimeMillis() >= endMillis &&
            RunSessionState.active.value?.finished != true &&
            repo.getRunningRun() != null
        ) {
            delay(1_000)
            if (
                navController.currentDestination?.route == Routes.IN_PROGRESS &&
                (repo.getRunningRun() == null || RunSessionState.active.value?.finished == true)
            ) {
                navigateHomeFromRun()
                if (repo.getRunningRun() == null) {
                    RunSessionState.setActive(null)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
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
                onStartRun = { intensity, durationMinutes, backgroundRun ->
                    RunForegroundService.start(
                        context,
                        intensity,
                        durationMinutes,
                        backgroundRun,
                    )
                },
            )
        }
        composable(Routes.IN_PROGRESS) {
            InProgressScreen(
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onCancelConfirmed = {
                    // Optimistic finish so foreground reconcile will not bounce back here
                    // while the service is still finalizing the Room row.
                    RunSessionState.update { it.copy(finished = true) }
                    RunForegroundService.cancel(context)
                    navigateHomeFromRun()
                },
                onFinishedNavigateHome = {
                    navigateHomeFromRun()
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
