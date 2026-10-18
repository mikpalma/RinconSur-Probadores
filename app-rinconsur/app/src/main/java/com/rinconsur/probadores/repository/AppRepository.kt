package com.rinconsur.probadores.repository

import androidx.room.withTransaction
import com.rinconsur.probadores.data.AppDatabase
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.model.RegistroOcupacion
import com.rinconsur.probadores.model.ReportePrenda
import com.rinconsur.probadores.model.Rol
import com.rinconsur.probadores.model.Sede
import com.rinconsur.probadores.model.Usuario
import com.rinconsur.probadores.model.UsuarioSede
import com.rinconsur.probadores.utils.TiempoUtils
import com.rinconsur.probadores.utils.Validadores
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Un usuario con los nombres de sus sedes (para la lista de personal). */
data class UsuarioConSedes(val usuario: Usuario, val sedes: List<String>, val todas: Boolean)

/** Un solo repositorio: aquí viven las reglas de negocio. Los cambios de estado van en transacción. */
class AppRepository(private val db: AppDatabase) {
    private val dao = db.dao()

    // ---------- Lecturas ----------
    fun sede(id: Long): Flow<Sede?> = dao.sede(id)
    fun todasLasSedes(): Flow<List<Sede>> = dao.sedes()
    fun probadores(sedeId: Long): Flow<List<Probador>> = dao.probadores(sedeId)

    /** RN5: el trabajador solo ve sus sedes autorizadas; el administrador ve todas. */
    fun sedesAutorizadas(u: Usuario): Flow<List<Sede>> =
        if (u.rol == Rol.ADMINISTRADOR) dao.sedes() else dao.sedesDe(u.id)

    /** RN16: el historial es de una sede. numero = null muestra todos los probadores. */
    fun historial(sedeId: Long, fecha: LocalDate, numero: Int?): Flow<List<RegistroOcupacion>> {
        val (desde, hasta) = TiempoUtils.rangoDelDia(fecha)
        return dao.historial(sedeId, desde, hasta, numero)
    }

    val personal: Flow<List<UsuarioConSedes>> =
        combine(dao.usuarios(), dao.usuarioSedes(), dao.sedes()) { usuarios, relaciones, sedes ->
            usuarios.map { u ->
                val nombres = relaciones.filter { it.usuarioId == u.id }
                    .mapNotNull { r -> sedes.firstOrNull { it.id == r.sedeId }?.nombre }
                UsuarioConSedes(u, nombres, todas = u.rol == Rol.ADMINISTRADOR)
            }
        }

    // ---------- Usuarios ----------
    suspend fun login(correo: String, clave: String): Usuario? {
        val u = dao.usuarioPorCorreo(correo.trim()) ?: return null
        return if (u.clave == Validadores.hash(clave)) u else null
    }

    suspend fun usuario(id: Long): Usuario? = dao.usuarioPorId(id)

    suspend fun correoRepetido(correo: String): Boolean = dao.usuarioPorCorreo(correo.trim()) != null

    /** RN3: solo el administrador crea trabajadores. El rol es siempre PERSONAL. */
    suspend fun crearPersonal(nombre: String, correo: String, clave: String, sedeIds: Set<Long>) {
        db.withTransaction {
            val id = dao.insertarUsuario(
                Usuario(nombre = nombre.trim(), correo = correo.trim().lowercase(), clave = Validadores.hash(clave), rol = Rol.PERSONAL)
            )
            dao.insertarUsuarioSedes(sedeIds.map { UsuarioSede(id, it) })
        }
    }

    // ---------- Probadores ----------
    /** RN7 y RN14: solo un probador LIBRE se puede ocupar. Guarda la hora de inicio (RN8). */
    suspend fun ocupar(id: Long): Boolean = db.withTransaction {
        val p = dao.probador(id) ?: return@withTransaction false
        if (p.estado != EstadoProbador.LIBRE) return@withTransaction false
        dao.actualizarProbador(p.copy(estado = EstadoProbador.OCUPADO, inicioOcupacion = System.currentTimeMillis()))
        true
    }

    /** RN13: al finalizar, vuelve a LIBRE o pasa a PRENDA_OLVIDADA si quedó una prenda. */
    suspend fun finalizar(id: Long, quedoPrenda: Boolean) {
        db.withTransaction {
            val p = dao.probador(id) ?: return@withTransaction
            if (p.estado == EstadoProbador.OCUPADO) cerrarUso(p, quedoPrenda)
        }
    }

    /** La incidencia se solucionó: PRENDA_OLVIDADA vuelve a LIBRE. */
    suspend fun solucionarPrenda(id: Long) {
        val p = dao.probador(id) ?: return
        if (p.estado == EstadoProbador.PRENDA_OLVIDADA) dao.actualizarProbador(p.copy(estado = EstadoProbador.LIBRE))
    }

    /** RN15: solo el administrador. Solo cambia un probador libre o fuera de servicio. */
    suspend fun cambiarHabilitado(id: Long, habilitado: Boolean) {
        val p = dao.probador(id) ?: return
        if (!habilitado && p.estado == EstadoProbador.LIBRE) {
            dao.actualizarProbador(p.copy(estado = EstadoProbador.FUERA_DE_SERVICIO))
        } else if (habilitado && p.estado == EstadoProbador.FUERA_DE_SERVICIO) {
            dao.actualizarProbador(p.copy(estado = EstadoProbador.LIBRE))
        }
    }

    suspend fun guardarTiempoAlerta(sedeId: Long, minutos: Int) = dao.cambiarTiempoAlerta(sedeId, minutos)

    /** Guarda el reporte. Si el probador seguía ocupado, también finaliza su uso (queda PRENDA_OLVIDADA). */
    suspend fun guardarReporte(id: Long, descripcion: String, fotoUri: String?) {
        db.withTransaction {
            val p = dao.probador(id) ?: return@withTransaction
            if (p.estado == EstadoProbador.OCUPADO) cerrarUso(p, quedoPrenda = true)
            dao.insertarReporte(
                ReportePrenda(sedeId = p.sedeId, numeroProbador = p.numero, descripcion = descripcion,
                    fotoUri = fotoUri, fechaMillis = System.currentTimeMillis())
            )
        }
    }

    // Guarda el registro del uso en el historial y actualiza el estado del probador
    private suspend fun cerrarUso(p: Probador, quedoPrenda: Boolean) {
        val inicio = p.inicioOcupacion ?: return
        dao.insertarRegistro(
            RegistroOcupacion(sedeId = p.sedeId, numeroProbador = p.numero, inicioMillis = inicio,
                finMillis = System.currentTimeMillis(), prendaOlvidada = quedoPrenda)
        )
        dao.actualizarProbador(
            p.copy(estado = if (quedoPrenda) EstadoProbador.PRENDA_OLVIDADA else EstadoProbador.LIBRE, inicioOcupacion = null)
        )
    }
}
