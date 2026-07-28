package com.herspace.app.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.herspace.app.data.api.TokenManager
import com.herspace.app.data.db.AppDatabase
import com.herspace.app.data.repository.PlaceRepository
import com.herspace.app.ui.auth.AuthScreen
import com.herspace.app.ui.auth.AuthViewModel
import com.herspace.app.ui.map.MapScreen
import com.herspace.app.ui.map.MapViewModel
import com.herspace.app.ui.vote.VoteScreen
import com.herspace.app.ui.vote.VoteViewModel
import com.herspace.app.util.LocationHelper

object Routes {
    const val AUTH = "auth"
    const val MAP = "map"
    const val VOTE = "vote"
}

@Composable
fun HerSpaceNavGraph() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val db = rememberDb(context)
    val repository = rememberRepository(db)
    val locationHelper = rememberLocationHelper(context)

    // 判断是否已登录，决定起始页
    val startDest = if (TokenManager.isLoggedIn()) Routes.MAP else Routes.AUTH

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Routes.AUTH) {
            val authViewModel: AuthViewModel = viewModel()
            AuthScreen(
                viewModel = authViewModel,
                onLoggedIn = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAP) {
            val mapViewModel: MapViewModel = viewModel(
                factory = MapViewModelFactory(repository, locationHelper)
            )
            MapScreen(
                viewModel = mapViewModel,
                onNavigateToVote = {
                    navController.navigate(Routes.VOTE)
                },
                onLogout = {
                    TokenManager.clear()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.MAP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.VOTE) {
            val voteViewModel: VoteViewModel = viewModel(
                factory = VoteViewModelFactory(repository, locationHelper)
            )
            VoteScreen(
                viewModel = voteViewModel,
                onBack = { navController.popBackStack() },
                onVoteComplete = { navController.popBackStack() }
            )
        }
    }
}

// --- ViewModel Factories ---

class MapViewModelFactory(
    private val repository: PlaceRepository,
    private val locationHelper: LocationHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(repository, locationHelper) as T
    }
}

class VoteViewModelFactory(
    private val repository: PlaceRepository,
    private val locationHelper: LocationHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VoteViewModel(repository, locationHelper) as T
    }
}

@Composable
private fun rememberDb(context: Context) = remember { AppDatabase.getInstance(context) }

@Composable
private fun rememberRepository(db: AppDatabase) = remember { PlaceRepository(db) }

@Composable
private fun rememberLocationHelper(context: Context) = remember { LocationHelper(context) }
