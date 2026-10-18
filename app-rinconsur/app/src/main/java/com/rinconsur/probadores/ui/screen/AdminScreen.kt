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
        ... (238 líneas restantes)