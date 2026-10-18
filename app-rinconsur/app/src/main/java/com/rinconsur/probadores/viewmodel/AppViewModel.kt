package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.Ajustes
import com.rinconsur.probadores.data.AppDatabase
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.model.RegistroOcupacion
import com.rinconsur.probadores.model.Sede
import com.rinconsur.probadores.model.Usuario
import com.rinconsur.probadores.repository.AppRepository
import com.rinconsur.probadores.repository.UsuarioConSedes
import com.rinconsur.probadores.utils.Validadores
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Sesión actual: quién entró y qué sede eligió. cargando = todavía se está leyendo del teléfono. */
data class Sesion(val cargando: Boolean = true, val usuario: Usuario? = null, val sedeId: Long? = null)

data class LoginUi(
    val cargando: Boolean = false,
    val errorCorreo: String? = null,
    val errorClave: String? = null,
    val errorGeneral: String? = null
)

/** Lo que muestra el Panel. "ahora" se refresca cada 30 s para actualizar los minutos. */
data class PanelUi(
    val sede: Sede? = null,
    val probadores: List<Probador> = emptyList(),
    val ahora: Long = System.currentTimeMillis()
) {
    val tiempoAlerta: Int get() = sede?.tiempoAlerta ?: 15
}

data class ErroresTrabajador(
    val nombre: String? = null,
    val correo: String? = null,
    val clave: String? = null,
    val confirmacion: String? = null,
    val sedes: String? = null
) {
    val hayErrores: Boolean get() = listOf(nombre, correo, clave, confirmacion, sedes).any { it != null }
}

/** Un solo ViewModel para toda la app (MVVM): guarda el estado y llama al repositorio. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(AppDatabase.getInstance(app))
    private val ajustes = Ajustes(app)

    // ---------- Sesión y sede ----------
    val sesion: StateFlow<Sesion> = combine(ajustes.usuarioId, ajustes.sedeId) { uid, sid ->
        Sesion(cargando = false, usuario = uid?.let { repo.usuario(it) }, sedeId = sid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Sesion())

    private val _login = MutableStateFlow(LoginUi())
    val login: StateFlow<LoginUi> = _login.asStateFlow()

    fun iniciarSesion(correo: String, clave: String) {
        val errorCorreo = Validadores.correo(correo)
        val errorClave = if (clave.isBlank()) "Ingresa la contraseña" else null
        if (errorCorreo != null || errorClave != null) {
            _login.value = LoginUi(errorCorreo = errorCorreo, errorClave = errorClave)
            return
        }
        viewModelScope.launch {
            _login.value = LoginUi(cargando = true)
            val usuario = repo.login(correo, clave)
            if (usuario == null) {
                _login.value = LoginUi(errorGeneral = "Correo o contraseña incorrectos")
            } else {
                ajustes.guardarSesion(usuario.id)
                _login.value = LoginUi()
            }
        }
    }

    fun cerrarSesion() { viewModelScope.launch { ajustes.cerrarSesion() } }
    fun seleccionarSede(id: Long) { viewModelScope.launch { ajustes.guardarSede(id) } }
    fun cambiarSede() { viewModelScope.launch { ajustes.limpiarSede() } }

    /** Solo las sedes que el usuario tiene autorizadas. */
    val sedesAutorizadas: StateFlow<List<Sede>> = sesion.map { it.usuario }.distinctUntilChanged()
        .flatMapLatest { u -> if (u == null) flowOf(emptyList()) else repo.sedesAutorizadas(u) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- Panel (también lo usa Configuración) ----------
    private val reloj = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    private val sedeIdActual = sesion.map { it.sedeId }.distinctUntilChanged()

    val panel: StateFlow<PanelUi> = sedeIdActual
        .flatMapLatest { id ->
            if (id == null) flowOf(PanelUi())
            else combine(repo.sede(id), repo.probadores(id), reloj) { sede, lista, ahora -> PanelUi(sede, lista, ahora) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PanelUi())

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun ocupar(p: Probador) {
        viewModelScope.launch {
            val ok = repo.ocupar(p.id)
            _mensajes.tryEmit(if (ok) "Probador ${p.numero} ocupado" else "Ese probador no está libre")
        }
    }

    fun finalizar(p: Probador, quedoPrenda: Boolean) {
        viewModelScope.launch {
            repo.finalizar(p.id, quedoPrenda)
            _mensajes.tryEmit(if (quedoPrenda) "Probador ${p.numero}: prenda olvidada" else "Probador ${p.numero} libre")
        }
    }

    fun solucionarPrenda(p: Probador) {
        viewModelScope.launch {
            repo.solucionarPrenda(p.id)
            _mensajes.tryEmit("Probador ${p.numero} libre")
        }
    }

    fun guardarReporte(p: Probador, descripcion: String, fotoUri: String?, onListo: () -> Unit) {
        viewModelScope.launch {
            repo.guardarReporte(p.id, descripcion.trim(), fotoUri)
            onListo()
        }
    }

    // ---------- Administración: Historial ----------
    private val _fecha = MutableStateFlow(LocalDate.now())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()

    private val _filtroProbador = MutableStateFlow<Int?>(null) // null = todos
    val filtroProbador: StateFlow<Int?> = _filtroProbador.asStateFlow()

    fun cambiarFecha(f: LocalDate) { _fecha.value = f }
    fun filtrarProbador(n: Int?) { _filtroProbador.value = n }

    val historial: StateFlow<List<RegistroOcupacion>> =
        combine(sedeIdActual, _fecha, _filtroProbador) { sede, fecha, numero -> Triple(sede, fecha, numero) }
            .flatMapLatest { (sede, fecha, numero) ->
                if (sede == null) flowOf(emptyList()) else repo.historial(sede, fecha, numero)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- Administración: Configuración (se guarda al instante) ----------
    fun cambiarTiempoAlerta(delta: Int) {
        val sede = panel.value.sede ?: return
        val nuevo = (sede.tiempoAlerta + delta).coerceIn(Validadores.ALERTA_MIN, Validadores.ALERTA_MAX)
        viewModelScope.launch { repo.guardarTiempoAlerta(sede.id, nuevo) }
    }

    fun cambiarHabilitado(p: Probador, habilitado: Boolean) {
        viewModelScope.launch { repo.cambiarHabilitado(p.id, habilitado) }
    }

    // ---------- Administración: Personal ----------
    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()
    fun buscar(texto: String) { _busqueda.value = texto }

    val personal: StateFlow<List<UsuarioConSedes>> = combine(repo.personal, _busqueda) { lista, texto ->
        val q = texto.trim()
        if (q.isEmpty()) lista
        else lista.filter { it.usuario.nombre.contains(q, ignoreCase = true) || it.usuario.correo.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todasLasSedes: StateFlow<List<Sede>> = repo.todasLasSedes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Crea un trabajador (rol PERSONAL). Devuelve los errores, o null si se creó bien. */
    suspend fun crearTrabajador(nombre: String, correo: String, clave: String, confirmacion: String, sedeIds: Set<Long>): ErroresTrabajador? {
        var errores = ErroresTrabajador(
            nombre = Validadores.nombre(nombre),
            correo = Validadores.correo(correo),
            clave = Validadores.clave(clave),
            confirmacion = Validadores.confirmacion(clave, confirmacion),
            sedes = Validadores.sedes(sedeIds)
        )
        if (errores.correo == null && repo.correoRepetido(correo)) errores = errores.copy(correo = "Este correo ya está registrado")
        if (errores.hayErrores) return errores
        repo.crearPersonal(nombre, correo, clave, sedeIds)
        _mensajes.emit("Trabajador creado")
        return null
    }
}
