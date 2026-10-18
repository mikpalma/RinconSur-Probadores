package com.rinconsur.probadores.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Rol
import com.rinconsur.probadores.repository.UsuarioConSedes
import com.rinconsur.probadores.ui.AppTopBar
import com.rinconsur.probadores.ui.CampoFormulario
import com.rinconsur.probadores.ui.theme.ColoresLibre
import com.rinconsur.probadores.ui.theme.ColoresOcupado
import com.rinconsur.probadores.ui.theme.ColoresOlvidada
import com.rinconsur.probadores.utils.TiempoUtils
import com.rinconsur.probadores.utils.Validadores
import com.rinconsur.probadores.viewmodel.AppViewModel
import com.rinconsur.probadores.viewmodel.ErroresTrabajador
import kotlinx.coroutines.launch

private val TITULOS = listOf("Historial", "Configuración", "Personal")

/** Vista 5: Administración (solo ADMINISTRADOR). Una sola vista con tres pestañas. */
@Composable
fun AdminScreen(vm: AppViewModel, onVolver: () -> Unit) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val panel by vm.panel.collectAsStateWithLifecycle()
    val usuario = sesion.usuario

    // Protección: si no es administrador, se vuelve atrás
    if (usuario == null || usuario.rol != Rol.ADMINISTRADOR) {
        LaunchedEffect(Unit) { onVolver() }
        return
    }

    var pestana by rememberSaveable { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            Column {
                AppTopBar("Administración · ${panel.sede?.nombre ?: ""}", usuario, vm::cerrarSesion, onVolver)
                TabRow(selectedTabIndex = pestana) {
                    TITULOS.forEachIndexed { i, titulo ->
                        Tab(selected = pestana == i, onClick = { pestana = i }, text = { Text(titulo, fontSize = 13.sp, maxLines = 1) })
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (pestana) {
                0 -> HistorialTab(vm)
                1 -> ConfiguracionTab(vm)
                else -> PersonalTab(vm)
            }
        }
    }
}

// ======================= HISTORIAL =======================
/** Usos finalizados de la sede, filtrados por fecha y por probador. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorialTab(vm: AppViewModel) {
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val filtro by vm.filtroProbador.collectAsStateWithLifecycle()
    val registros by vm.historial.collectAsStateWithLifecycle()
    var mostrarFecha by remember { mutableStateOf(false) }
    var menuProbador by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        // --- Filtros ---
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { mostrarFecha = true }, modifier = Modifier.weight(1f)) {
                    Text("Fecha: ${TiempoUtils.fechaCorta(fecha)}", fontSize = 13.sp)
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { menuProbador = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Probador: ${filtro?.toString() ?: "Todos"}", fontSize = 13.sp)
                    }
                    DropdownMenu(expanded = menuProbador, onDismissRequest = { menuProbador = false }) {
                        DropdownMenuItem(text = { Text("Todos") }, onClick = { vm.filtrarProbador(null); menuProbador = false })
                        (1..6).forEach { n ->
                            DropdownMenuItem(text = { Text("Probador $n") }, onClick = { vm.filtrarProbador(n); menuProbador = false })
                        }
                    }
                }
            }
        }

        // --- Tabla ---
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp)) {
                listOf("Inicio", "Término", "Probador", "Tiempo").forEach {
                    Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Text("Incidencia", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            }
            HorizontalDivider()
        }
        if (registros.isEmpty()) {
            item { Text("Sin registros para esta fecha.", modifier = Modifier.padding(top = 16.dp)) }
        }
        items(registros, key = { it.id }) { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(TiempoUtils.hora(r.inicioMillis), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(TiempoUtils.hora(r.finMillis), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${r.numeroProbador}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${TiempoUtils.minutosEntre(r.inicioMillis, r.finMillis)} min", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Box(Modifier.weight(1.5f)) {
                    if (r.prendaOlvidada) {
                        Text("Prenda olvidada", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ColoresOlvidada.texto,
                            modifier = Modifier.background(ColoresOlvidada.fondo, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    } else {
                        Text("Sin incidencia", fontSize = 12.sp)
                    }
                }
            }
            HorizontalDivider()
        }
    }

    if (mostrarFecha) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = TiempoUtils.utcDesdeFecha(fecha))
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { vm.cambiarFecha(TiempoUtils.fechaDesdeUtc(it)) }
                    mostrarFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}

// ======================= CONFIGURACIÓN =======================
/** Tiempo de alerta de la sede y habilitar o deshabilitar probadores (se guarda al instante). */
@Composable
private fun ConfiguracionTab(vm: AppViewModel) {
    val panel by vm.panel.collectAsStateWithLifecycle()
    val tiempo = panel.tiempoAlerta

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Configuración de ${panel.sede?.nombre ?: "la sede"}", fontWeight = FontWeight.Bold, fontSize = 17.sp)

        // --- Tiempo de alerta ---
        Text("Tiempo de alerta", fontWeight = FontWeight.SemiBold)
        Text("Avisar cuando un probador lleve más de:", fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(onClick = { vm.cambiarTiempoAlerta(-1) }, enabled = tiempo > Validadores.ALERTA_MIN) {
                Icon(Icons.Filled.Remove, "Restar un minuto")
            }
            Text("$tiempo", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp).padding(horizontal = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(vertical = 6.dp))
            OutlinedIconButton(onClick = { vm.cambiarTiempoAlerta(1) }, enabled = tiempo < Validadores.ALERTA_MAX) {
                Icon(Icons.Filled.Add, "Sumar un minuto")
            }
            Text("  minutos")
        }
        Text("Es solo un aviso para el personal: el cliente puede seguir usando el probador.", fontSize = 12.sp)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        // --- Probadores de la sede ---
        Text("Probadores de la sede", fontWeight = FontWeight.SemiBold)
        panel.probadores.forEach { p ->
            // Solo se cambia un probador libre o fuera de servicio
            val editable = p.estado == EstadoProbador.LIBRE || p.estado == EstadoProbador.FUERA_DE_SERVICIO
            val habilitado = p.estado != EstadoProbador.FUERA_DE_SERVICIO
            val etiqueta = if (!editable) p.estado.etiqueta else if (habilitado) "Habilitado" else "Fuera de servicio"
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Probador ${p.numero}", modifier = Modifier.weight(1f))
                Switch(checked = habilitado, onCheckedChange = { vm.cambiarHabilitado(p, it) }, enabled = editable)
                Text(etiqueta, fontSize = 12.sp, modifier = Modifier.width(110.dp).padding(start = 8.dp))
            }
        }
    }
}

// ======================= PERSONAL =======================
/** Lista de trabajadores, buscador y formulario "Nuevo trabajador". */
@Composable
private fun PersonalTab(vm: AppViewModel) {
    val personal by vm.personal.collectAsStateWithLifecycle()
    val busqueda by vm.busqueda.collectAsStateWithLifecycle()
    var mostrarFormulario by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { mostrarFormulario = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
            Text("  Nuevo trabajador")
        }
        CampoFormulario(busqueda, vm::buscar, null, placeholder = "Buscar por nombre o correo...", icono = Icons.Filled.Search)

        LazyColumn {
            items(personal, key = { it.usuario.id }) { FilaPersonal(it) }
        }
    }
    if (mostrarFormulario) FormularioTrabajador(vm) { mostrarFormulario = false }
}

@Composable
private fun FilaPersonal(item: UsuarioConSedes) {
    val u = item.usuario
    val esAdmin = u.rol == Rol.ADMINISTRADOR
    val colores = if (esAdmin) ColoresOcupado else ColoresLibre
    val sedes = if (item.todas) "Todas" else item.sedes.joinToString(", ").ifEmpty { "Sin sedes" }

    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text(u.nombre.split(" ").take(2).joinToString("") { it.take(1) }.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(u.nombre, fontWeight = FontWeight.SemiBold)
            Text(u.correo, fontSize = 12.sp)
            Text("Sedes: $sedes", fontSize = 12.sp)
        }
        Text(if (esAdmin) "ADMIN" else "PERSONAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colores.texto,
            modifier = Modifier.background(colores.fondo, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
    }
    HorizontalDivider()
}

/** Formulario de nuevo trabajador. El rol PERSONAL se asigna solo; hay que elegir al menos una sede. */
@Composable
private fun FormularioTrabajador(vm: AppViewModel, onCerrar: () -> Unit) {
    val sedesDisponibles by vm.todasLasSedes.collectAsStateWithLifecycle()
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var elegidas by remember { mutableStateOf(setOf<Long>()) }
    var errores by remember { mutableStateOf(ErroresTrabajador()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo trabajador") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CampoFormulario(nombre, { nombre = it }, "Nombre completo", icono = Icons.Filled.Person, error = errores.nombre)
                CampoFormulario(correo, { correo = it }, "Correo electrónico", icono = Icons.Filled.Email,
                    tipoTeclado = KeyboardType.Email, error = errores.correo)
                CampoFormulario(clave, { clave = it }, "Contraseña", icono = Icons.Filled.Lock, esPassword = true, error = errores.clave)
                CampoFormulario(confirmacion, { confirmacion = it }, "Confirmar contraseña", icono = Icons.Filled.Lock,
                    esPassword = true, error = errores.confirmacion)

                Text("Sedes autorizadas", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                sedesDisponibles.forEach { sede ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sede.id in elegidas,
                            onCheckedChange = { marcada -> elegidas = if (marcada) elegidas + sede.id else elegidas - sede.id })
                        Text(sede.nombre)
                    }
                }
                if (errores.sedes != null) Text(errores.sedes!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Text("Rol: PERSONAL (se asigna automáticamente)", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val resultado = vm.crearTrabajador(nombre, correo, clave, confirmacion, elegidas)
                    if (resultado == null) onCerrar() else errores = resultado
                }
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
