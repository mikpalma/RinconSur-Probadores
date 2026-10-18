package com.rinconsur.probadores.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinconsur.probadores.model.Rol
import com.rinconsur.probadores.viewmodel.AppViewModel

/** Vista 2: Selección de sede. Solo aparecen las sedes autorizadas (el administrador ve todas). */
@Composable
fun SedeScreen(vm: AppViewModel) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val sedes by vm.sedesAutorizadas.collectAsStateWithLifecycle()
    val usuario = sesion.usuario ?: return

    val titulo = if (usuario.rol == Rol.ADMINISTRADOR) "Selecciona la sede a administrar" else "Selecciona la sede donde trabajarás"

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text("RINCÓN SUR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        if (sedes.isEmpty()) {
            Text("No tienes sedes autorizadas. Pide al Administrador que te asigne una.", textAlign = TextAlign.Center)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(sedes, key = { it.id }) { sede ->
                // Tarjeta de una sede
                Card(onClick = { vm.seleccionarSede(sede.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storefront, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text("Rincón Sur", fontSize = 13.sp)
                            Text(sede.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }
        }
        OutlinedButton(onClick = vm::cerrarSesion) { Text("Cerrar sesión") }
    }
}
