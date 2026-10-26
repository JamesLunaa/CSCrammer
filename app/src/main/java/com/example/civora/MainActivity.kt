package com.example.civora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.civora.domain.MockKind
import com.example.civora.ui.home.HomeScreen
import com.example.civora.ui.mock.MockScreen
import com.example.civora.ui.mock.MockSetupScreen
import com.example.civora.ui.mock.MockViewModel
import com.example.civora.ui.practice.PracticeSetupScreen
import com.example.civora.ui.practice.PracticeSetupViewModel
import com.example.civora.ui.quiz.QuizScreen
import com.example.civora.ui.quiz.QuizSource
import com.example.civora.ui.quiz.QuizViewModel
import com.example.civora.ui.results.ResultsScreen
import com.example.civora.ui.results.ResultsViewModel
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.ThemeMode

private object Routes {
    const val Home = "home"
    const val PracticeSetup = "practice-setup"
    const val MockSetup = "mock-setup"
    const val Mock = "mock/{kind}"
    const val Results = "results/{sessionId}"
    fun mock(kind: MockKind) = "mock/${kind.key}"
    fun results(sessionId: Long) = "results/$sessionId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CivoraApp).container
        setContent {
            val mode by container.preferences.themeMode.collectAsStateWithLifecycle(ThemeMode.Dark)
            CivoraTheme(mode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val toResults: (Long) -> Unit = { id ->
                        nav.navigate(Routes.results(id)) { popUpTo(Routes.Home) }
                    }
                    NavHost(nav, startDestination = Routes.Home) {
                        composable(Routes.Home) {
                            val overall by container.progress.overall.collectAsStateWithLifecycle(null)
                            HomeScreen(
                                overall = overall,
                                onQuickReview = { nav.navigate(QuizSource.route(QuizSource.Quick)) },
                                onPractice = { nav.navigate(Routes.PracticeSetup) },
                                onMock = { nav.navigate(Routes.MockSetup) },
                            )
                        }
                        composable(Routes.PracticeSetup) {
                            val vm = viewModel<PracticeSetupViewModel>(
                                factory = viewModelFactory { initializer { PracticeSetupViewModel(container) } },
                            )
                            PracticeSetupScreen(
                                vm,
                                onBack = { nav.popBackStack() },
                                onStart = { nav.navigate(QuizSource.route(it)) },
                            )
                        }
                        composable(
                            QuizSource.ROUTE,
                            arguments = listOf(
                                navArgument("subject") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("topics") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("difficulties") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("count") { type = NavType.IntType; defaultValue = 10 },
                            ),
                        ) { entry ->
                            val args = entry.arguments
                            val source = QuizSource.fromArgs(
                                mode = args?.getString("mode"),
                                subject = args?.getString("subject"),
                                topics = args?.getString("topics"),
                                difficulties = args?.getString("difficulties"),
                                count = args?.getInt("count") ?: 10,
                            )
                            if (source == null) {
                                LaunchedEffect(Unit) { nav.popBackStack() }
                            } else {
                                val vm = viewModel<QuizViewModel>(
                                    factory = viewModelFactory { initializer { QuizViewModel(container, source) } },
                                )
                                QuizScreen(vm, onExit = { nav.popBackStack() }, onFinished = toResults)
                            }
                        }
                        composable(Routes.MockSetup) {
                            MockSetupScreen(
                                onBack = { nav.popBackStack() },
                                onStart = { nav.navigate(Routes.mock(it)) { popUpTo(Routes.MockSetup) { inclusive = true } } },
                            )
                        }
                        composable(Routes.Mock) { entry ->
                            val kind = MockKind.fromKey(entry.arguments?.getString("kind").orEmpty())
                            val vm = viewModel<MockViewModel>(
                                factory = viewModelFactory { initializer { MockViewModel(container, kind) } },
                            )
                            MockScreen(vm, onExit = { nav.popBackStack() }, onFinished = toResults)
                        }
                        composable(
                            Routes.Results,
                            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                        ) { entry ->
                            val id = entry.arguments?.getLong("sessionId") ?: 0L
                            val vm = viewModel<ResultsViewModel>(
                                factory = viewModelFactory { initializer { ResultsViewModel(container, id) } },
                            )
                            ResultsScreen(vm, onDone = { nav.popBackStack(Routes.Home, inclusive = false) })
                        }
                    }
                }
            }
        }
    }
}
