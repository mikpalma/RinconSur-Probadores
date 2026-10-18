package com.rinconsur.probadores.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.model.Usuario
import com.rinconsur.probadores.ui.theme.AzulMarino
import com.rinconsur.probadores.ui.theme.ColoresAlerta
import com.rinconsur.probadores.ui.theme.coloresDeEstado
import com.rinconsur.probadores.utils.TiempoUtils

// ---------- Barra superior ----------
/** Barra azul con "RINCÓN SUR" y la sede. El ícono de perfil muestra el usuario y "Cerrar sesión". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(sede: String, usuario: Usuario, onCerrarSesion: () -> Unit, onVolver: (() -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AzulMarino, titleContentColor = Color.White,
            navigationIconContentColor = Color.White, actionIconContentColor = Color.White
        ),
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            } else {
                Icon(Icons.Filled.Storefront, null, Modifier.padding(start = 16.dp, end = 4.dp))
            }
        },
        title = {
            Column {
                Text("RINCÓN SUR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(sede, fontSize = 12.sp)
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.AccountCircle, "Perfil") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("${usuario.nombre} · ${usuario.rol.etiqueta}") }, onClick = {}, enabled = false)
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Cerrar sesión") }, onClick = { menu = false; onCerrarSesion() })
                }
            }
        }
    )
}

// ---------- Menú "Más" ----------
@Composable
private fun MenuMas(
    abierto: Boolean, onCerrar: () -> Unit, esAdmin: Boolean,
    onCambiarSede: () -> Unit, onAdministracion: () -> Unit, onCerrarSesion: () -> Unit
) {
    DropdownMenu(expanded = abierto, onDismissRequest = onCerrar) {
        DropdownMenuItem(text = { Text("Cambiar sede") }, onClick = { onCerrar(); onCambiarSede() })
        if (esAdmin) {
            DropdownMenuItem(text = { Text("Administración") }, onClick = { onCerrar(); onAdministracion() })
        }
        DropdownMenuItem(text = { Text("Cerrar sesión") }, onClick = { onCerrar(); onCerrarSesion() })
    }
}

// ---------- Navegación: barra inferior (Compact y Medium) ----------
/** Probadores | Reporte | Más. "Más" abre Cambiar sede, Administración (solo admin) y Cerrar sesión. */
@Composable
fun AppBottomBar(
    seleccionado: Int, // 0 = Probadores, 1 = Reporte
    esAdmin: Boolean,
    onProbadores: () -> Unit,
    onReporte: () -> Unit,
    onCambiarSede: () -> Unit,
    onAdministracion: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    NavigationBar {
        NavigationBarItem(selected = seleccionado == 0, onClick = onProbadores,
            icon = { Icon(Icons.Filled.Checkroom, null) }, label = { Text("Probadores") })
        NavigationBarItem(selected = seleccionado == 1, onClick = onReporte,
            icon = { Icon(Icons.Filled.Description, null) }, label = { Text("Reporte") })
        NavigationBarItem(selected = false, onClick = { menu = true }, label = { Text("Más") },
            icon = {
                Box {
                    Icon(Icons.Filled.MoreHoriz, null)
                    MenuMas(menu, { menu = false }, esAdmin, onCambiarSede, onAdministracion, onCerrarSesion)
                }
            })
    }
}

// ---------- Navegación: menú lateral (Expanded) ----------
@Composable
private fun AppRail(
    seleccionado: Int,
    esAdmin: Boolean,
    onProbadores: () -> Unit,
    onReporte: () -> Unit,
    onCambiarSede: () -> Unit,
    onAdministracion: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    NavigationRail {
        NavigationRailItem(selected = seleccionado == 0, onClick = onProbadores,
            icon = { Icon(Icons.Filled.Checkroom, null) }, label = { Text("Probadores") })
        NavigationRailItem(selected = seleccionado == 1, onClick = onReporte,
            icon = { Icon(Icons.Filled.Description, null) }, label = { Text("Reporte") })
        NavigationRailItem(selected = false, onClick = { menu = true }, label = { Text("Más") },
            icon = {
                Box {
                    Icon(Icons.Filled.MoreHoriz, null)
                    MenuMas(menu, { menu = false }, esAdmin, onCambiarSede, onAdministracion, onCerrarSesion)
                }
            })
    }
}

/**
 * Adapta la navegación al tamaño de pantalla:
 * Compact y Medium usan la barra inferior; Expanded usa el menú lateral.
 */
@Composable
fun PantallaConNavegacion(
    ancho: WindowWidthSizeClass,
    seleccionado: Int,
    esAdmin: Boolean,
    onProbadores: () -> Unit,
    onReporte: () -> Unit,
    onCambiarSede: () -> Unit,
    onAdministracion: () -> Unit,
    onCerrarSesion: () -> Unit,
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    val expandido = ancho == WindowWidthSizeClass.Expanded
    Scaffold(
        topBar = topBar,
        snackbarHost = snackbarHost,
        bottomBar = {
            if (!expandido) {
                AppBottomBar(seleccionado, esAdmin, onProbadores, onReporte, onCambiarSede, onAdministracion, onCerrarSesion)
            }
        }
    ) { padding ->
        if (expandido) {
            Row(Modifier.padding(padding)) {
                AppRail(seleccionado, esAdmin, onProbadores, onReporte, onCambiarSede, onAdministracion, onCerrarSesion)
                Box(Modifier.weight(1f)) { contenido(PaddingValues()) }
            }
        } else {
            contenido(padding)
        }
    }
}

// ---------- Campo de texto ----------
/** Campo reutilizable: con ícono, mensaje de error y (si es contraseña) el ojito. */
@Composable
fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String?,
    error: String? = null,
    icono: ImageVector? = null,
    esPassword: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null
) {
    var verClave by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.fillMaxWidth(),
        label = if (etiqueta != null) { { Text(etiqueta) } } else null,
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        leadingIcon = if (icono != null) { { Icon(icono, null) } } else null,
        trailingIcon = if (esPassword) {
            {
                IconButton(onClick = { verClave = !verClave }) {
                    Icon(if (verClave) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar u ocultar contraseña")
                }
            }
        } else null,
        visualTransformation = if (esPassword && !verClave) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esPassword) KeyboardType.Password else tipoTeclado, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp)
    )
}

// ---------- Fila de un probador ----------
/**
 * Fila de un probador en el Panel. El color cambia con animación según el estado.
 * Un OCUPADO que pasó el tiempo de alerta se ve rojo (sigue siendo OCUPADO).
 */
@Composable
fun ProbadorCard(p: Probador, ahora: Long, tiempoAlerta: Int, onOcupar: () -> Unit, onAbrirDetalle: () -> Unit) {
    val minutos = p.inicioOcupacion?.let { TiempoUtils.minutosEntre(it, ahora) } ?: 0
    val alerta = p.estado == EstadoProbador.OCUPADO && minutos >= tiempoAlerta
    val colores = coloresDeEstado(p.estado, alerta)
    val fondo by animateColorAsState(if (p.estado == EstadoProbador.LIBRE) Color.White else colores.fondo, label = "fondo")
    val abreDetalle = p.estado == EstadoProbador.OCUPADO || p.estado == EstadoProbador.PRENDA_OLVIDADA
    val forma = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(fondo)
            .border(1.dp, colores.principal.copy(alpha = 0.35f), forma)
            .clickable(enabled = abreDetalle, onClick = onAbrirDetalle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PuntoEstado(colores.principal, pulsa = alerta)
        Text("Probador ${p.numero}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 12.dp))

        when (p.estado) {
            EstadoProbador.LIBRE -> {
                Text("LIBRE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colores.texto)
                Spacer(Modifier.width(10.dp))
                Button(onClick = onOcupar, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Ocupar", fontSize = 13.sp)
                }
            }
            EstadoProbador.OCUPADO -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text("OCUPADO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colores.texto)
                    Text("$minutos min", fontSize = 12.sp, color = colores.texto)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver detalle")
            }
            EstadoProbador.PRENDA_OLVIDADA -> {
                Text("PRENDA OLVIDADA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colores.texto)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver detalle")
            }
            EstadoProbador.FUERA_DE_SERVICIO ->
                Text("FUERA DE SERVICIO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colores.texto)
        }
    }
}

// Punto de color del estado. Si hay alerta, parpadea (cambia de transparencia) para llamar la atención
@Composable
private fun PuntoEstado(color: Color, pulsa: Boolean) {
    val alfa = if (pulsa) {
        val transicion = rememberInfiniteTransition(label = "parpadeo")
        val valor by transicion.animateFloat(
            initialValue = 1f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alfa"
        )
        valor
    } else 1f
    Box(Modifier.size(14.dp).alpha(alfa).background(color, CircleShape))
}

// ---------- Detalle del probador (Bottom Sheet) ----------
/** Se abre sobre el Panel; no es una pantalla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleProbadorSheet(
    p: Probador,
    ahora: Long,
    tiempoAlerta: Int,
    onFinalizar: () -> Unit,
    onSolucionar: () -> Unit,
    onReportar: () -> Unit,
    onCerrar: () -> Unit
) {
    val ocupado = p.estado == EstadoProbador.OCUPADO
    val minutos = p.inicioOcupacion?.let { TiempoUtils.minutosEntre(it, ahora) } ?: 0
    val alerta = ocupado && minutos >= tiempoAlerta
    val colores = coloresDeEstado(p.estado, alerta)

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Probador ${p.numero}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                p.estado.etiqueta.uppercase(), color = colores.texto, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(colores.fondo).padding(horizontal = 10.dp, vertical = 4.dp)
            )

            if (ocupado) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(32.dp))
                    Column {
                        Text("Tiempo de uso", fontSize = 13.sp)
                        Text("$minutos min", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth()) { Text("Finalizar uso") }
                if (alerta) {
                    // Aviso de tiempo de alerta superado
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ColoresAlerta.fondo).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = ColoresAlerta.principal, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Este probador lleva más de $tiempoAlerta minutos ocupado.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColoresAlerta.texto)
                            Text("Se recomienda verificar su situación.", fontSize = 12.sp, color = ColoresAlerta.texto)
                        }
                    }
                }
            } else if (p.estado == EstadoProbador.PRENDA_OLVIDADA) {
                Text("Hay una prenda olvidada pendiente en este probador.")
                Button(onClick = onSolucionar, modifier = Modifier.fillMaxWidth()) { Text("Incidencia solucionada") }
                OutlinedButton(onClick = onReportar, modifier = Modifier.fillMaxWidth()) { Text("Registrar reporte") }
            }
            TextButton(onClick = onCerrar, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
        }
    }
}
