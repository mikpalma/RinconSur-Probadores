package com.rinconsur.probadores.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------- Enums ----------
/** Estados oficiales. "Con alerta" NO es un estado: es un OCUPADO que pasó el tiempo de alerta. */
enum class EstadoProbador(val etiqueta: String) {
    LIBRE("Libre"),
    OCUPADO("Ocupado"),
    PRENDA_OLVIDADA("Prenda olvidada"),
    FUERA_DE_SERVICIO("Fuera de servicio")
}

enum class Rol(val etiqueta: String) { ADMINISTRADOR("Administrador"), PERSONAL("Personal") }

// ---------- Tablas de la base de datos (Room) ----------
/** Sucursal (ficticia). Cada sede guarda su propio tiempo de alerta. */
@Entity(tableName = "sedes")
data class Sede(@PrimaryKey val id: Long, val nombre: String, val tiempoAlerta: Int = 15)

/** Cuenta de la app. La clave se guarda protegida (hash). */
@Entity(tableName = "usuarios", indices = [Index(value = ["correo"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: Rol
)

/** Qué sedes tiene autorizadas cada trabajador (el administrador accede a todas). */
@Entity(tableName = "usuario_sede", primaryKeys = ["usuarioId", "sedeId"])
data class UsuarioSede(val usuarioId: Long, val sedeId: Long)

/** Un probador de una sede. Los minutos no se guardan: se calculan desde inicioOcupacion. */
@Entity(tableName = "probadores", indices = [Index(value = ["sedeId", "numero"], unique = true)])
data class Probador(
    @PrimaryKey val id: Long,
    val numero: Int,
    val sedeId: Long,
    val estado: EstadoProbador = EstadoProbador.LIBRE,
    val inicioOcupacion: Long? = null
)

/** Se guarda cuando termina un uso (alimenta el Historial). */
@Entity(tableName = "registros")
data class RegistroOcupacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sedeId: Long,
    val numeroProbador: Int,
    val inicioMillis: Long,
    val finMillis: Long,
    val prendaOlvidada: Boolean = false
)

/** Reporte de prenda olvidada (sin datos personales de clientes). */
@Entity(tableName = "reportes_prenda")
data class ReportePrenda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sedeId: Long,
    val numeroProbador: Int,
    val descripcion: String,
    val fotoUri: String? = null,
    val fechaMillis: Long
)
