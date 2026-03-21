package com.basefit.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.basefit.app.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Plan : Screen("plan")
    object Record : Screen("record")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object AddExercise : Screen("add_exercise")
    object EditExercise : Screen("edit_exercise/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "edit_exercise/$exerciseId"
    }
    object AddWeekPlan : Screen("add_week_plan")
    object AddChallenge : Screen("add_challenge")
    object CheckIn : Screen("check_in/{exerciseId}/{date}") {
        fun createRoute(exerciseId: Long, date: Long) = "check_in/$exerciseId/$date"
    }
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_detail/$exerciseId"
    }
}

@Composable
fun BaseFitNavGraph(
    navController: NavHostController = rememberNavController(),
    bottomBarPadding: PaddingValues = PaddingValues()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPlan = { navController.navigate(Screen.Plan.route) },
                onNavigateToCheckIn = { exerciseId, date ->
                    navController.navigate(Screen.CheckIn.createRoute(exerciseId, date))
                },
                bottomBarPadding = bottomBarPadding
            )
        }
        
        composable(Screen.Plan.route) {
            PlanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddWeekPlan = { navController.navigate(Screen.AddWeekPlan.route) },
                onNavigateToAddChallenge = { navController.navigate(Screen.AddChallenge.route) }
            )
        }
        
        composable(Screen.Record.route) {
            RecordScreen(
                onNavigateToCheckIn = { exerciseId, date ->
                    navController.navigate(Screen.CheckIn.createRoute(exerciseId, date))
                },
                bottomBarPadding = bottomBarPadding
            )
        }
        
        composable(Screen.Stats.route) {
            StatsScreen(bottomBarPadding = bottomBarPadding)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExercise = { navController.navigate(Screen.AddExercise.route) }
            )
        }
        
        composable(Screen.AddExercise.route) {
            AddExerciseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AddWeekPlan.route) {
            AddWeekPlanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AddChallenge.route) {
            AddChallengeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.CheckIn.route
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toLongOrNull() ?: 0L
            val date = backStackEntry.arguments?.getString("date")?.toLongOrNull() ?: System.currentTimeMillis()
            CheckInScreen(
                exerciseId = exerciseId,
                date = date,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
