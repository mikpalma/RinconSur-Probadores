package com.rinconsur.probadores.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Rol
import com.rinconsur.probadores.ui.PantallaConNavegacion
import com.rinconsur.probadores.ui.AppTopBar
import com.rinconsur.probadores.ui.CampoFormulario
import com.rinconsur.probadores.utils.CamaraUtils
import com.rinconsur.probadores.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Vista 4: Reporte de prenda olvidada (probador, foto de cámara o galería, descripción). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(vm: AppViewModel, ancho: WindowWidthSizeClass, probadorIdInicial: Long, onProbadores: () -> Unit, onAdministracion: () -> Unit) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val panel by vm.panel.collectAsStateWithLifecycle()
    val usuario = sesion.usuario ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var probadorId by rememberSaveable { mutableStateOf(probadorIdInicial) }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var foto by rememberSaveable { mutableStateOf<String?>(null) }
    var uriCamara by rememberSaveable { mutableStateOf<Uri?>(null) }
    var intentado by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    // Solo se reporta en probadores ocupados o con prenda olvidada de la sede
    val elegibles = panel.probadores.filter { it.estado == EstadoProbador.OCUPADO || it.estado == EstadoProbador.PRENDA_OLVIDADA }
    val elegido = panel.probadores.firstOrNull { it.id == probadorId }

    // Cámara y galería (recursos nativos del teléfono)
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) foto = uriCamara?.toString()
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            foto = withContext(Dispatchers.IO) { CamaraUtils.copiarDeGaleria(context, uri) }.toString()
        }
    }

    // Los errores solo se muestran después de intentar guardar
    val errorProbador = if (intentado && elegido == null) "Selecciona un probador" else null
    val errorContenido = if (intentado && descripcion.isBlank() && foto == null) "Agrega una descripción o una foto" else null

    fun guardar() {
        intentado = true
        if (elegido != null && (descripcion.isNotBlank() || foto != null)) {
            vm.guardarReporte(elegido, descripcion, foto, onListo = onProbadores)
        }
    }

    PantallaConNavegacion(
        ancho = ancho,
        seleccionado = 1,
        esAdmin = usuario.rol == Rol.ADMINISTRADOR,
        onProbadores = onProbadores,
        onReporte = {},
        onCambiarSede = vm::cambiarSede,
        onAdministracion = onAdministracion,
        onCerrarSesion = vm::cerrarSesion,
        topBar = { AppTopBar("Sede: ${panel.sede?.nombre ?: ""}", usuario, vm::cerrarSesion) }
    ) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Registrar prenda olvidada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // --- Foto: cámara o galería ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { val uri = CamaraUtils.crearUriFoto(context); uriCamara = uri; camara.launch(uri) },
                    modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp)
                ) { Icon(Icons.Filled.PhotoCamera, null, Modifier.size(20.dp)); Text("  Tomar foto") }
                OutlinedButton(
                    onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp)
                ) { Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(20.dp)); Text("  De la galería") }
            }

            // Vista previa con "X" para quitar la foto
            foto?.let { uri ->
                Box(Modifier.size(110.dp)) {
                    AsyncImage(model = uri, contentDescription = "Foto de la prenda", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                    IconButton(onClick = { foto = null },
                        modifier = Modifier.align(Alignment.TopEnd).size(26.dp).background(Color(0xCC000000), CircleShape)) {
                        Icon(Icons.Filled.Close, "Quitar foto", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // --- Probador de la sede ---
            ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                OutlinedTextField(
                    value = elegido?.let { "Probador ${it.numero}" } ?: "",
                    onValueChange = {}, readOnly = true,
                    label = { Text("Probador") }, placeholder = { Text("Selecciona un probador") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) },
                    isError = errorProbador != null,
                    supportingText = if (errorProbador != null) { { Text(errorProbador) } } else null,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    elegibles.forEach { p ->
                        DropdownMenuItem(text = { Text("Probador ${p.numero}") }, onClick = { probadorId = p.id; menu = false })
                    }
                }
            }
            if (elegibles.isEmpty()) {
                Text("No hay probadores ocupados ni con prenda olvidada en esta sede.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            // --- Descripción ---
            CampoFormulario(descripcion, { descripcion = it }, "Descripción (opcional)",
                placeholder = "Ej: polera negra, chaqueta, etc.", singleLine = false, minLines = 3, error = errorContenido)

            Button(onClick = ::guardar, enabled = elegibles.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                Text("REGISTRAR REPORTE", fontWeight = FontWeight.Bold)
            }
        }
    }
}
