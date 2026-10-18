package com.rinconsur.probadores.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rinconsur.probadores.model.EstadoProbador

val AzulMarino = Color(0xFF0D3B66)

/ Trío de colores de un estado: fondo suave, color principal y texto. */
data class ColoresEstado(val fondo: Color, val principal: Color, val texto: Color)

val ColoresLibre = ColoresEstado(Color(0xFFE5F5E9), Color(0xFF2E9E4F), Color(0xFF1E7B3A))     // verde
val ColoresOcupado = ColoresEstado(Color(0xFFE3EEFC), Color(0xFF2F6FDB), Color(0xFF2457B8))   // azul
val ColoresAlerta = ColoresEstado(Color(0xFFFDE5E5), Color(0xFFE5534B), Color(0xFFC62828))    // rojo
val ColoresOlvidada = ColoresEstado(Color(0xFFFFF0D9), Color(0xFFF08C00), Color(0xFFC76A00))  // naranjo
val ColoresFuera = ColoresEstado(Color(0xFFEDEDED), Color(0xFF9E9E9E), Color(0xFF555555))     // gris

/ Elige el color de un probador. "Alerta" es un OCUPADO que pasó el tiempo de alerta. */
fun coloresDeEstado(estado: EstadoProbador, alerta: Boolean = false): ColoresEstado = when {
    alerta -> ColoresAlerta
    estado == EstadoProbador.LIBRE -> ColoresLibre
    estado == EstadoProbador.OCUPADO -> ColoresOcupado
    estado == EstadoProbador.PRENDA_OLVIDADA -> ColoresOlvidada
    else -> ColoresFuera
}

private val Esquema = lightColorScheme(
    primary = AzulMarino,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EEFC),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2F7),
    outline = Color(0xFFB7C2D0)
)

@Composable
fun RinconSurTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, content = content)
}