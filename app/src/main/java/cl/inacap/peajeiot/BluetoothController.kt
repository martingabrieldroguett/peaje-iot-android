package cl.inacap.peajeiot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Data class para guardar los datos de cada dispositivo vinculado
data class DispositivoBt(val nombre: String, val mac: String, val device: BluetoothDevice)

// Sealed class para manejar los estados de la conexion
sealed class EstadoConexion {
    object Desconectado : EstadoConexion()
    data class Conectando(val nombre: String) : EstadoConexion()
    data class Conectado(val nombre: String) : EstadoConexion()
    data class Error(val detalle: String) : EstadoConexion()
}

object BluetoothController {
    // UUID estandar para comunicacion serial Bluetooth (HC-05 y Android)
    private val UUID_SERIAL: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val handler = Handler(Looper.getMainLooper())

    // Variables de estado para actualizar la interfaz
    var estadoBt by mutableStateOf<EstadoConexion>(EstadoConexion.Desconectado)
    var estadoValla by mutableStateOf("Cerrada (0°)")
    var switchAbierto by mutableStateOf(false)
    var distanciaAuto by mutableIntStateOf(10)
    var estadoTarjeta by mutableStateOf("Esperando tarjeta...")
    var modoServidor by mutableStateOf(false)

    // Lista mutable donde se van guardando los logs que llegan por Bluetooth
    val historialLogs = mutableStateListOf("[SETUP] App lista. Esperando conexion...")

    private var socketActual: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var salidaDatos: OutputStream? = null
    private var escuchando = false

    // Credenciales del supervisor (la clave se compara usando hash SHA-256 por seguridad ISO)
    private const val USUARIO_ADMIN = "supervisor"
    private val CLAVE_HASH = encriptarSha256("peaje2026")

    fun validarLogin(user: String, pass: String): Boolean {
        return user.trim().lowercase() == USUARIO_ADMIN && encriptarSha256(pass) == CLAVE_HASH
    }

    private fun encriptarSha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @SuppressLint("MissingPermission")
    fun buscarVinculados(context: Context): List<DispositivoBt> {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = manager?.adapter
        val lista = adapter?.bondedDevices ?: emptySet()

        return lista.map {
            DispositivoBt(
                nombre = it.name ?: "Sin nombre",
                mac = it.address ?: "00:00:00:00:00:00",
                device = it
            )
        }
    }

    // Conectar como cliente (hacia el HC-05 del Arduino o hacia el Telefono 2)
    @SuppressLint("MissingPermission")
    fun conectarDispositivo(context: Context, disp: DispositivoBt) {
        desconectar()
        modoServidor = false
        estadoBt = EstadoConexion.Conectando(disp.nombre)
        agregarLog("[BT] Conectando con ${disp.nombre}...")

        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter

        Thread {
            try {
                val socket = disp.device.createRfcommSocketToServiceRecord(UUID_SERIAL)
                adapter?.cancelDiscovery()
                socket.connect()
                manejarConexion(socket, disp.nombre)
            } catch (e: IOException) {
                handler.post {
                    estadoBt = EstadoConexion.Error("No se pudo conectar")
                    agregarLog("[ERROR] Fallo al conectar con ${disp.nombre}")
                }
            }
        }.start()
    }

    // Modo servidor para conectar dos celulares Android entre si
    @SuppressLint("MissingPermission")
    fun activarModoServidor(context: Context) {
        desconectar()
        modoServidor = true
        estadoBt = EstadoConexion.Conectando("Esperando otro Android...")
        agregarLog("[SERVIDOR] Esperando conexion del otro telefono...")

        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter

        Thread {
            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord("PeajeApp", UUID_SERIAL)
                val socket = serverSocket?.accept()
                serverSocket?.close()
                socket?.let {
                    manejarConexion(it, "Telefono Supervisor")
                }
            } catch (e: IOException) {
                handler.post {
                    estadoBt = EstadoConexion.Error("Servidor detenido")
                }
            }
        }.start()
    }

    private fun manejarConexion(socket: BluetoothSocket, nombreDisp: String) {
        socketActual = socket
        salidaDatos = socket.outputStream
        escuchando = true

        handler.post {
            estadoBt = EstadoConexion.Conectado(nombreDisp)
            agregarLog("[BT] Conectado a $nombreDisp")
        }

        // Hilo para leer los mensajes que llegan del Arduino o del otro telefono
        Thread {
            try {
                val lector = BufferedReader(InputStreamReader(socket.inputStream))
                while (escuchando) {
                    val linea = lector.readLine() ?: break
                    procesarMensaje(linea.trim())
                }
            } catch (e: IOException) {
                handler.post {
                    estadoBt = EstadoConexion.Desconectado
                    agregarLog("[BT] Se perdio la conexion")
                }
            }
        }.start()
    }

    private fun procesarMensaje(mensaje: String) {
        if (mensaje.isEmpty()) return

        handler.post {
            agregarLog("Recibido: $mensaje")

            // Revisamos los mismos mensajes que manda el Arduino en el codigo de la Unidad 1
            when {
                mensaje.contains("Abierta", ignoreCase = true) -> {
                    estadoValla = "Abierta (90°)"
                    switchAbierto = true
                }
                mensaje.contains("Cerrada", ignoreCase = true) -> {
                    estadoValla = "Cerrada (0°)"
                    switchAbierto = false
                }
                mensaje.contains("Distancia:", ignoreCase = true) -> {
                    val num = mensaje.filter { it.isDigit() }.toIntOrNull()
                    num?.let { distanciaAuto = it }
                }
                mensaje.contains("Tarjeta VALIDA", ignoreCase = true) -> {
                    estadoTarjeta = "Tarjeta VALIDA - Buen Viaje"
                }
                mensaje.contains("Tarjeta INVALIDA", ignoreCase = true) -> {
                    estadoTarjeta = "ACCESO DENEGADO - Tarjeta Invalida"
                }
            }
        }

        // Si este celular hace de Simulador (App 2), responde igual que el Arduino
        if (modoServidor) {
            when (mensaje.uppercase()) {
                "A" -> {
                    enviarDatos("[VALLA] Abierta (manual)", revisarComando = false)
                    handler.post {
                        estadoValla = "Abierta (90°)"
                        switchAbierto = true
                    }
                }
                "C" -> {
                    enviarDatos("[VALLA] Cerrada (manual)", revisarComando = false)
                    handler.post {
                        estadoValla = "Cerrada (0°)"
                        switchAbierto = false
                    }
                }
                "D" -> {
                    enviarDatos("Distancia: $distanciaAuto cm", revisarComando = false)
                }
            }
        }
    }

    // Envia comandos validando que solo sean A, C o D (seguridad OT igual que en el Arduino)
    fun enviarDatos(texto: String, revisarComando: Boolean = true) {
        val cmd = texto.trim()
        if (cmd.isEmpty()) return

        if (revisarComando && !modoServidor) {
            val permitidos = listOf("A", "C", "D")
            if (cmd.uppercase() !in permitidos) {
                agregarLog("[CMD] Comando no valido. Use: A, C, D")
                return
            }
        }

        val out = salidaDatos
        if (out == null) {
            // Si no hay Bluetooth conectado, probamos los cambios en la misma pantalla
            agregarLog("[LOCAL] Simulando envio: $cmd")
            when (cmd.uppercase()) {
                "A" -> {
                    estadoValla = "Abierta (90°)"
                    switchAbierto = true
                }
                "C" -> {
                    estadoValla = "Cerrada (0°)"
                    switchAbierto = false
                }
                "D" -> agregarLog("Distancia: $distanciaAuto cm")
            }
            return
        }

        Thread {
            try {
                out.write((cmd + "\n").toByteArray())
                out.flush()
                handler.post { agregarLog("Enviado: $cmd") }
            } catch (e: IOException) {
                handler.post { agregarLog("[ERROR] No se pudo enviar el comando") }
            }
        }.start()
    }

    fun agregarLog(texto: String) {
        val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        historialLogs.add(0, "[$hora]$texto")
    }

    fun desconectar() {
        escuchando = false
        try {
            serverSocket?.close()
            socketActual?.close()
        } catch (_: IOException) {}
        salidaDatos = null
        estadoBt = EstadoConexion.Desconectado
    }
}
