package com.rinconsur.probadores.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinconsur.probadores.ui.CampoFormulario
import com.rinconsur.probadores.viewmodel.AppViewModel

/** Vista 1: Login. No existe "Crear cuenta": solo ingresan cuentas creadas por el Administrador. */
@Composable
fun LoginScreen(vm: AppViewModel) {
    val ui by vm.login.collectAsStateWithLifecycle()
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Icon(Icons.Filled.Checkroom, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text("RINCÓN SUR", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("TIENDA DEPARTAMENTAL", fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(36.dp))
        Text("Iniciar sesión", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CampoFormulario(correo, { correo = it }, null, placeholder = "Correo electrónico",
                icono = Icons.Filled.Email, tipoTeclado = KeyboardType.Email, error = ui.errorCorreo)
            CampoFormulario(clave, { clave = it }, null, placeholder = "Contraseña", icono = Icons.Filled.Lock,
                esPassword = true, imeAction = ImeAction.Done, onImeAction = { vm.iniciarSesion(correo, clave) },
                error = ui.errorClave)
        }
        if (ui.errorGeneral != null) {
            Text(ui.errorGeneral!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.iniciarSesion(correo, clave) },
            enabled = !ui.cargando,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (ui.cargando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("INGRESAR", fontSize = 16.sp)
        }
    }
}

