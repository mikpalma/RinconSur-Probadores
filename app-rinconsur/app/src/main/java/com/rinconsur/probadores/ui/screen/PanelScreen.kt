package com.rinconsur.probadores.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Rol
import com.rinconsur.probadores.ui.PantallaConNavegacion
import com.rinconsur.probadores.ui.AppTopBar
import com.rinconsur.probadores.ui.DetalleProbadorSheet
import com.rinconsur.probadores.ui.ProbadorCard
import com.rinconsur.probadores.ui.theme.ColoresAlerta
import com.rinconsur.probadores.ui.theme.ColoresEstado
import com.rinconsur.probadores.ui.theme.ColoresLibre
import com.rinconsur.probadores.ui.theme.ColoresOcupado
import com.rinconsur.probadores.utils.TiempoUtils
import com.rinconsur.probadores.viewmodel.AppViewModel

/** Vista 3: Panel de probadores de la sede elegida. Es la pantalla principal de trabajo. */
@Composable
fun PanelScreen(
    vm: AppViewModel,
    ancho: WindowWidthSizeClass,
    onReportar: (Long) -> Unit, // -1 = sin probador elegido
    onAdministracion: () -> Unit
) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val panel by vm.panel.collectAsStateWithLifecycle()
    val usuario = sesion.usuario ?: return

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    // Qué ventana está abierta (se guarda solo el id del probador)
    var idDetalle by rememberSaveable { mutableStateOf<Long?>(null) }
    var idPregunta by rememberSaveable { mutableStateOf<Long?>(null) }
    val detalle = panel.probadores.firstOrNull { it.id == idDetalle }
    val pregunta = panel.probadores.firstOrNull { it.id == idPregunta }

    // Resumen: los "con alerta" son ocupados que superaron el tiempo de alerta
    val ocupados = panel.probadores.filter { it.estado == EstadoProbador.OCUPADO }
    val conAlerta = ocupados.count {
        it.inicioOcupacion != null && TiempoUtils.minutosEntre(it.inicioOcupacion, panel.ahora) >= panel.tiempoAlerta
    }
    val libres = panel.probadores.count { it.estado == EstadoProbador.LIBRE }

    // Columnas según el tamaño de pantalla: Compact 1, Medium 2, Expanded 3
    val columnas = when (ancho) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        else -> 3
    }

    PantallaConNavegacion(
        ancho = ancho,
        seleccionado = 0,
        esAdmin = usuario.rol == Rol.ADMINISTRADOR,
        onProbadores = {},
        onReporte = { onReportar(-1L) },
        onCambiarSede = vm::cambiarSede,
        onAdministracion = onAdministracion,
        onCerrarSesion = vm::cerrarSesion,
        topBar = { AppTopBar("Sede: ${panel.sede?.nombre ?: ""}", usuario, vm::cerrarSesion) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnas),
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
            ... (60 líneas restantes)

