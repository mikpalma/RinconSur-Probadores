package com.rinconsur.probadores.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ajustes")

/** DataStore: guarda solo dos datos pequeños, la sesión iniciada y la sede elegida. */
class Ajustes(private val context: Context) {
    private val claveUsuario = longPreferencesKey("usuario_id")
    private val claveSede = longPreferencesKey("sede_id")

    val usuarioId: Flow<Long?> = context.dataStore.data.map { it[claveUsuario] }
    val sedeId: Flow<Long?> = context.dataStore.data.map { it[claveSede] }

    suspend fun guardarSesion(id: Long) { context.dataStore.edit { it[claveUsuario] = id } }
    suspend fun guardarSede(id: Long) { context.dataStore.edit { it[claveSede] = id } }
    suspend fun limpiarSede() { context.dataStore.edit { it.remove(claveSede) } }

    /** Al cerrar sesión se borran la sesión y la sede elegida. */
    suspend fun cerrarSesion() {
        context.dataStore.edit {
            it.remove(claveUsuario)
            it.remove(claveSede)
        }
    }
}
