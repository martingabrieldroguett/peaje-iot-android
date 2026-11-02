package cl.inacap.peajeiot

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.inacap.peajeiot.Screens.ActivadorScreen
import cl.inacap.peajeiot.Screens.LoginScreen
import cl.inacap.peajeiot.Screens.MonitoreoScreen

@Composable
fun NavegacionApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable(route = "login") {
            LoginScreen(navController)
        }
        composable(route = "monitoreo/{nombre}") {
            val nombre = it.arguments?.getString("nombre") ?: "Supervisor"
            MonitoreoScreen(navController, nombre)
        }
        composable(route = "activador/{nombre}") {
            val nombre = it.arguments?.getString("nombre") ?: "Supervisor"
            ActivadorScreen(navController, nombre)
        }
    }
}
