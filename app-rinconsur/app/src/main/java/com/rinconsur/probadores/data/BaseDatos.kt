package com.rinconsur.probadores.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.withTransaction
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

/** Un solo DAO con todas las consultas de la app. */
@Dao
interface AppDao {
    // --- Sedes ---
    @Query("SELECT * FROM sedes ORDER BY id")
    fun sedes(): Flow<List<Sede>>

    // Solo las sedes que el administrador autorizó a ese trabajador
    @Query("SELECT s.* FROM sedes s INNER JOIN usuario_sede us ON us.sedeId = s.id WHERE us.usuarioId = :usuarioId ORDER BY s.id")
    fun sedesDe(usuarioId: Long): Flow<List<Sede>>

    @Query("SELECT * FROM sedes WHERE id = :id")
    fun sede(id: Long): Flow<Sede?>

    @Query("UPDATE sedes SET tiempoAlerta = :minutos WHERE id = :id")
    suspend fun cambiarTiempoAlerta(id: Long, minutos: Int)

    // --- Usuarios ---
    @Query("SELECT * FROM usuarios ORDER BY nombre COLLATE NOCASE")
    fun usuarios(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios WHERE correo = :correo COLLATE NOCASE LIMIT 1")
    suspend fun usuarioPorCorreo(correo: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun usuarioPorId(id: Long): Usuario?

    @Query("SELECT * FROM usuario_sede")
    fun usuarioSedes(): Flow<List<UsuarioSede>>

    @Insert
    suspend fun insertarUsuario(u: Usuario): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarUsuarioSedes(lista: List<UsuarioSede>)

    // --- Probadores (siempre de UNA sede) ---
    @Query("SELECT * FROM probadores WHERE sedeId = :sedeId ORDER BY numero")
    fun probadores(sedeId: Long): Flow<List<Probador>>

    @Query("SELECT * FROM probadores WHERE id = :id")
    suspend fun probador(id: Long): Probador?

    @Update
    suspend fun actualizarProbador(p: Probador)

    // --- Historial y reportes ---
    @Insert
    suspend fun insertarRegistro(r: RegistroOcupacion)

    // numero = null significa "todos los probadores"
    @Query(
        "SELECT * FROM registros WHERE sedeId = :sedeId AND inicioMillis >= :desde AND inicioMillis < :hasta " +
                "AND (:numero IS NULL OR numeroProbador = :numero) ORDER BY inicioMillis DESC"
    )
    fun historial(sedeId: Long, desde: Long, hasta: Long, numero: Int?): Flow<List<RegistroOcupacion>>

    @Insert
    suspend fun insertarReporte(r: ReportePrenda)

    // --- Solo para los datos de ejemplo ---
    @Query("SELECT COUNT(*) FROM sedes")
    suspend fun contarSedes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSedes(lista: List<Sede>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProbadores(lista: List<Probador>)

    @Insert
    suspend fun insertarRegistros(lista: List<RegistroOcupacion>)
}

/** Le enseña a Room a guardar los enums como texto. */
class Converters {
    @TypeConverter fun estadoATexto(e: EstadoProbador): String = e.name
    @TypeConverter fun textoAEstado(t: String): EstadoProbador = EstadoProbador.valueOf(t)
    @TypeConverter fun rolATexto(r: Rol): String = r.name
    @TypeConverter fun textoARol(t: String): Rol = Rol.valueOf(t)
}

@Database(
    entities = [Sede::class, Usuario::class, UsuarioSede::class, Probador::class, RegistroOcupacion::class, ReportePrenda::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        // Una sola base de datos para toda la app
        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rinconsur.db")
                    .build().also { instancia = it }
            }
    }
}

/** Datos FICTICIOS de la demostración: 3 sedes de 6 probadores, un administrador y dos trabajadores. */
object DatosIniciales {
    suspend fun sembrarSiVacio(db: AppDatabase) {
        val dao = db.dao()
        if (dao.contarSedes() > 0) return
        db.withTransaction {
            dao.insertarSedes(listOf(Sede(1, "Santiago Norte"), Sede(2, "Santiago Centro"), Sede(3, "Santiago Oriente")))

            // 6 probadores por sede. Santiago Norte trae un ejemplo de cada estado
            val ahora = System.currentTimeMillis()
            dao.insertarProbadores((1L..3L).flatMap { sede ->
                (1..6).map { n ->
                    val id = (sede - 1) * 6 + n
                    if (sede != 1L) Probador(id, n, sede)
                    else when (n) {
                        2 -> Probador(id, n, sede, EstadoProbador.OCUPADO, ahora - 8 * 60_000L)
                        3 -> Probador(id, n, sede, EstadoProbador.OCUPADO, ahora - 22 * 60_000L)
                        5 -> Probador(id, n, sede, EstadoProbador.PRENDA_OLVIDADA)
                        6 -> Probador(id, n, sede, EstadoProbador.FUERA_DE_SERVICIO)
                        else -> Probador(id, n, sede)
                    }
                }
            })

            // Usuarios: el administrador accede a todas las sedes (no necesita filas en usuario_sede)
            dao.insertarUsuario(Usuario(nombre = "Ana Castillo", correo = "ana@rinconsur.cl", clave = Validadores.hash("Admin123!"), rol = Rol.ADMINISTRADOR))
            val maria = dao.insertarUsuario(Usuario(nombre = "María Pérez", correo = "maria@rinconsur.cl", clave = Validadores.hash("Personal123"), rol = Rol.PERSONAL))
            val juan = dao.insertarUsuario(Usuario(nombre = "Juan López", correo = "juan@rinconsur.cl", clave = Validadores.hash("Personal123"), rol = Rol.PERSONAL))
            dao.insertarUsuarioSedes(listOf(UsuarioSede(maria, 1), UsuarioSede(juan, 1), UsuarioSede(juan, 2)))

            // Historial de ejemplo (de hoy) en Santiago Norte
            dao.insertarRegistros(listOf(
                RegistroOcupacion(sedeId = 1, numeroProbador = 1, inicioMillis = TiempoUtils.hoyA(8, 15), finMillis = TiempoUtils.hoyA(8, 32)),
                RegistroOcupacion(sedeId = 1, numeroProbador = 2, inicioMillis = TiempoUtils.hoyA(9, 10), finMillis = TiempoUtils.hoyA(9, 28)),
                RegistroOcupacion(sedeId = 1, numeroProbador = 3, inicioMillis = TiempoUtils.hoyA(10, 5), finMillis = TiempoUtils.hoyA(10, 20), prendaOlvidada = true)
            ))
            dao.insertarReporte(ReportePrenda(sedeId = 1, numeroProbador = 5, descripcion = "Polera negra encontrada dentro del probador.", fechaMillis = ahora))
        }
    }
}
