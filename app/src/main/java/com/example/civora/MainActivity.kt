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
import com.example.civora.ui.custom.MyQuestionsScreen
import com.example.civora.ui.custom.MyQuestionsViewModel
import com.example.civora.ui.custom.QuestionEditorScreen
import com.example.civora.ui.custom.QuestionEditorViewModel
import com.example.civora.ui.home.HomeScreen
import com.example.civora.ui.mock.MockScreen
import com.example.civora.ui.mock.MockSetupScreen
import com.example.civora.ui.mock.MockViewModel
import com.example.civora.ui.practice.PracticeSetupScreen
import com.example.civora.ui.practice.PracticeSetupViewModel
import com.example.civora.ui.quiz.QuizScreen
import com.example.civora.ui.quiz.QuizSource
import com.example.civora.ui.quiz.QuizViewModel
import com.example.civora.ui.review.ReviewScreen
import com.example.civora.ui.review.ReviewViewModel
import com.example.civora.ui.results.ResultsScreen
import com.example.civora.ui.results.ResultsViewModel
import com.example.civora.ui.settings.SettingsScreen
import com.example.civora.ui.settings.SettingsViewModel
import com.example.civora.ui.stats.StatsScreen
import com.example.civora.ui.stats.StatsViewModel
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.ThemeMode

private object Routes {
    const val Home = "home"
    const val PracticeSetup = "practice-setup"
    const val Review = "review"
    const val Stats = "stats"
    const val Settings = "settings"
    const val MockSetup = "mock-setup"
    const val Mock = "mock/{kind}"
    const val Results = "results/{sessionId}"
    const val MyQuestions = "my-questions"
    const val Editor = "question-editor?id={id}"
    fun editor(id: Int? = null) = if (id == null) "question-editor" else "question-editor?id=$id"
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
                                onReview = { nav.navigate(Routes.Review) },
                                onMyQuestions = { nav.navigate(Routes.MyQuestions) },
                                onStats = { nav.navigate(Routes.Stats) },
                                onSettings = { nav.navigate(Routes.Settings) },
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
                        composable(Routes.Review) {
                            val vm = viewModel<ReviewViewModel>(
                                factory = viewModelFactory { initializer { ReviewViewModel(container) } },
                            )
                            ReviewScreen(
                                vm,
                                onBack = { nav.popBackStack() },
                                onDrill = { nav.navigate(QuizSource.route(it)) },
                            )
                        }
                        composable(Routes.MyQuestions) {
                            val vm = viewModel<MyQuestionsViewModel>(
                                factory = viewModelFactory { initializer { MyQuestionsViewModel(container) } },
                            )
                            MyQuestionsScreen(
                                vm,
                                onBack = { nav.popBackStack() },
                                onAdd = { nav.navigate(Routes.editor()) },
                                onEdit = { nav.navigate(Routes.editor(it)) },
                                onDrill = { nav.navigate(QuizSource.route(QuizSource.Custom)) },
                            )
                        }
                        composable(
                            Routes.Editor,
                            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 }),
                        ) { entry ->
                            val id = entry.arguments?.getInt("id")?.takeIf { it >= 0 }
                            val vm = viewModel<QuestionEditorViewModel>(
                                factory = viewModelFactory { initializer { QuestionEditorViewModel(container, id) } },
                            )
                            QuestionEditorScreen(vm, onClose = { nav.popBackStack() })
                        }
                        composable(Routes.Settings) {
                            val vm = viewModel<SettingsViewModel>(
                                factory = viewModelFactory { initializer { SettingsViewModel(container) } },
                            )
                            SettingsScreen(vm, onBack = { nav.popBackStack() })
                        }
                        composable(Routes.Stats) {
                            val vm = viewModel<StatsViewModel>(
                                factory = viewModelFactory { initializer { StatsViewModel(container) } },
                            )
                            StatsScreen(
                                vm,
                                onBack = { nav.popBackStack() },
                                onOpenSession = { nav.navigate(Routes.results(it)) },
                            )
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
