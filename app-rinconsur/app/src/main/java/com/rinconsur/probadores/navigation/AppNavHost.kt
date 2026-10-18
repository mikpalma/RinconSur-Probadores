package com.rinconsur.probadores.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rinconsur.probadores.ui.screen.AdminScreen
import com.rinconsur.probadores.ui.screen.LoginScreen
import com.rinconsur.probadores.ui.screen.PanelScreen
import com.rinconsur.probadores.ui.screen.ReporteScreen
import com.rinconsur.probadores.ui.screen.SedeScreen
import com.rinconsur.probadores.viewmodel.AppViewModel

/** Nombres de las 5 pantallas (el detalle del probador no es pantalla: es un Bottom Sheet). */
object Routes {
    const val LOGIN = "login"
    const val SEDE = "sede"
    const val PANEL = "panel"
    const val ADMIN = "admin"
    const val REPORTE = "reporte?probadorId={probadorId}"

    fun reporte(probadorId: Long) = "reporte?probadorId=$probadorId"
}

/** Flujo: Login -> Selección de sede -> Panel -> (Reporte | Administración). */
@Composable
fun AppNavHost(vm: AppViewModel, ancho: WindowWidthSizeClass) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()

    // Mientras se lee la sesión guardada no se muestra nada (evita parpadeos)
    if (sesion.cargando) {
        Box(Modifier.fillMaxSize())
        return
    }

    val nav = rememberNavController()
    val hayUsuario = sesion.usuario != null
    val haySede = sesion.sedeId != null

    // Pantalla inicial según la sesión guardada (se calcula una sola vez)
    val inicio = remember { if (!hayUsuario) Routes.LOGIN else if (!haySede) Routes.SEDE else Routes.PANEL }

    NavHost(navController = nav, startDestination = inicio) {
        composable(Routes.LOGIN) { LoginScreen(vm) }
        composable(Routes.SEDE) { SedeScreen(vm) }
        composable(Routes.PANEL) {
            PanelScreen(
                vm = vm,
                ancho = ancho,
                onReportar = { id -> nav.navigate(Routes.reporte(id)) { launchSingleTop = true } },
                onAdministracion = { nav.navigate(Routes.ADMIN) { launchSingleTop = true } }
            )
        }
        composable(
            route = Routes.REPORTE,
            arguments = listOf(navArgument("probadorId") { type = NavType.LongType; defaultValue = -1L })
        ) { entrada ->
            ReporteScreen(
                vm = vm,
                ancho = ancho,
                probadorIdInicial = entrada.arguments?.getLong("probadorId") ?: -1L,
                onProbadores = { irAlPanel(nav) },
                onAdministracion = { nav.navigate(Routes.ADMIN) { launchSingleTop = true } }
            )
        }
        composable(Routes.ADMIN) { AdminScreen(vm = vm, onVolver = { nav.popBackStack() }) }
    }

    // Reacciona a la sesión y a la sede: iniciar/cerrar sesión, elegir/cambiar sede
    LaunchedEffect(hayUsuario, haySede) {
        val actual = nav.currentDestination?.route ?: return@LaunchedEffect
        if (!hayUsuario && actual != Routes.LOGIN) {
            irLimpiando(nav, Routes.LOGIN)
        } else if (hayUsuario && !haySede && actual != Routes.SEDE) {
            irLimpiando(nav, Routes.SEDE)
        } else if (hayUsuario && haySede && (actual == Routes.LOGIN || actual == Routes.SEDE)) {
            irLimpiando(nav, Routes.PANEL)
        }
    }
}

// Vuelve al Panel dejando una sola copia en la pila
private fun irAlPanel(nav: NavHostController) {
    nav.navigate(Routes.PANEL) {
        popUpTo(Routes.PANEL) { inclusive = false }
        launchSingleTop = true
    }
}

// Va a un destino borrando todo lo anterior (no se puede volver atrás)
private fun irLimpiando(nav: NavHostController, ruta: String) {
    nav.navigate(ruta) { popUpTo(nav.graph.id) { inclusive = true } }
}
