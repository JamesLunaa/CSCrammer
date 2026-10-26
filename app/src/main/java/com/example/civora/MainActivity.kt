package com.example.civora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.civora.ui.home.HomeScreen
import com.example.civora.ui.quiz.QuizModes
import com.example.civora.ui.quiz.QuizScreen
import com.example.civora.ui.quiz.QuizViewModel
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.ThemeMode

private object Routes {
    const val Home = "home"
    const val Quiz = "quiz/{mode}"
    fun quiz(mode: String) = "quiz/$mode"
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
                    NavHost(nav, startDestination = Routes.Home) {
                        composable(Routes.Home) {
                            HomeScreen(onQuickReview = { nav.navigate(Routes.quiz(QuizModes.Quick)) })
                        }
                        composable(Routes.Quiz) { entry ->
                            val quizMode = entry.arguments?.getString("mode").orEmpty()
                            val vm = viewModel<QuizViewModel>(
                                factory = viewModelFactory { initializer { QuizViewModel(container, quizMode) } },
                            )
                            QuizScreen(vm, onExit = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
