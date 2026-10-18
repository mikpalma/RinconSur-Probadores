package com.rinconsur.probadores.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Patterns
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Validaciones: cada función devuelve el mensaje de error, o null si está bien. */
object Validadores {
    const val ALERTA_MIN = 5
    const val ALERTA_MAX = 60

    fun nombre(v: String): String? = if (v.trim().length < 3) "Ingresa el nombre completo" else null

    fun correo(v: String): String? = when {
        v.isBlank() -> "Ingresa el correo electrónico"
        !Patterns.EMAIL_ADDRESS.matcher(v.trim()).matches() -> "Correo no válido"
        else -> null
    }

    fun clave(v: String): String? = if (v.length < 6) "Mínimo 6 caracteres" else null

    fun confirmacion(clave: String, otra: String): String? =
        if (clave != otra) "Las contraseñas no coinciden" else null

    fun sedes(elegidas: Set<Long>): String? = if (elegidas.isEmpty()) "Asigna al menos una sede" else null

    /** Convierte la contraseña en un código SHA-256 para no guardarla en texto plano. */
    fun hash(clave: String): String =
        MessageDigest.getInstance("SHA-256").digest(clave.toByteArray()).joinToString("") { "%02x".format(it) }
}

/** Ayudas para horas, fechas y minutos. */
object TiempoUtils {
    private val zona: ZoneId get() = ZoneId.systemDefault()

    fun minutosEntre(inicio: Long, fin: Long): Int = ((fin - inicio) / 60_000L).toInt().coerceAtLeast(0)

    fun hora(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zona).format(DateTimeFormatter.ofPattern("HH:mm"))

    fun fechaCorta(f: LocalDate): String = f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    /** Hoy a una hora determinada (para los datos de ejemplo). */
    fun hoyA(h: Int, m: Int): Long = LocalDate.now().atTime(h, m).atZone(zona).toInstant().toEpochMilli()

    /** Inicio y fin de un día, para filtrar el historial. */
    fun rangoDelDia(f: LocalDate): Pair<Long, Long> =
        f.atStartOfDay(zona).toInstant().toEpochMilli() to f.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()

    // El selector de fecha de Material trabaja en UTC
    fun fechaDesdeUtc(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    fun utcDesdeFecha(f: LocalDate): Long = f.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

/** Guarda las fotos de prendas olvidadas dentro de la app. */
object CamaraUtils {
    private fun nuevoArchivo(context: Context): File {
        val carpeta = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: File(context.filesDir, "Pictures")
        carpeta.mkdirs()
        return File(carpeta, "prenda_${System.currentTimeMillis()}.jpg")
    }

    private fun aUri(context: Context, f: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)

    /** Archivo vacío donde la cámara guardará la foto. */
    fun crearUriFoto(context: Context): Uri = aUri(context, nuevoArchivo(context))

    /** Copia la foto elegida de la galería a la carpeta de la app (el permiso de galería es temporal). */
    fun copiarDeGaleria(context: Context, origen: Uri): Uri {
        val destino = nuevoArchivo(context)
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        return aUri(context, destino)
    }
}
