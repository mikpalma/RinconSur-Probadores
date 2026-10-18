package com.rinconsur.probadores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rinconsur.probadores.ui.theme.RinconSurTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Inicia la app: tema + navegación. */
class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Tamaño de la pantalla: Compact (celular), Medium (tablet vertical) o Expanded (tablet horizontal)
            val ancho = calculateWindowSizeClass(this@MainActivity).widthSizeClass

            // La primera vez se cargan las 3 sedes, sus probadores y las cuentas de ejemplo
            var datosListos by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    DatosIniciales.sembrarSiVacio(
                        AppDatabase.getInstance(
                            applicationContext
                        )
                    )
                }
                datosListos = true
            }

            RinconSurTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (datosListos) AppNavHost(vm, ancho)
                }
            }
        }
    }
}