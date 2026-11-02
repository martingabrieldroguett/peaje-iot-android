package cl.inacap.peajeiot.Screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cl.inacap.peajeiot.BluetoothController
import cl.inacap.peajeiot.DispositivoBt
import cl.inacap.peajeiot.EstadoConexion

@Composable
fun MonitoreoScreen(navController: NavHostController, nombre: String) {
    val context = LocalContext.current
    var verDispositivos by rememberSaveable { mutableStateOf(false) }
    var filtrarValla by rememberSaveable { mutableStateOf(false) }
    var listaBt by remember { mutableStateOf(emptyList<DispositivoBt>()) }

    // Pedir permisos de Bluetooth en Android 12 o superior
    val pedirPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        listaBt = BluetoothController.buscarVinculados(context)
        verDispositivos = true
    }

    val textoEstado = when (val estado = BluetoothController.estadoBt) {
        is EstadoConexion.Desconectado -> "Desconectado"
        is EstadoConexion.Conectando -> "Conectando: ${estado.nombre}"
        is EstadoConexion.Conectado -> "Conectado a ${estado.nombre}"
        is EstadoConexion.Error -> "Error: ${estado.detalle}"
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Monitoreo Peaje - Hola $nombre",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta para conectar Bluetooth
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Bluetooth: $textoEstado")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    pedirPermisos.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                } else {
                                    listaBt = BluetoothController.buscarVinculados(context)
                                    verDispositivos = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Buscar BT")
                        }

                        Button(
                            onClick = { BluetoothController.activarModoServidor(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modo App 2")
                        }
                    }

                    if (verDispositivos) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Toque un dispositivo para conectar:")
                        listaBt.forEach { disp ->
                            Text(
                                text = "${disp.nombre} (${disp.mac})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.LightGray)
                                    .padding(8.dp)
                                    .clickable {
                                        verDispositivos = false
                                        BluetoothController.conectarDispositivo(context, disp)
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta con los datos recibidos del peaje
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Datos del Peaje en Tiempo Real",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Estado Valla: ${BluetoothController.estadoValla}")
                    Text(text = "Sensor Ultrasonico: ${BluetoothController.distanciaAuto} cm")
                    Text(text = "Lector RFID: ${BluetoothController.estadoTarjeta}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Checkbox para filtrar la lista de logs
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = filtrarValla,
                    onCheckedChange = { filtrarValla = it }
                )
                Text("Ver solo movimientos de valla")
            }

            val logsFiltrados = if (filtrarValla) {
                BluetoothController.historialLogs.filter {
                    it.contains("Valla", ignoreCase = true) ||
                            it.contains("Enviado", ignoreCase = true) ||
                            it.contains("Recibido", ignoreCase = true)
                }
            } else {
                BluetoothController.historialLogs
            }

            // Lista de logs con LazyColumn como en el PPT
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logsFiltrados) { log ->
                    Text(text = log, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate(route = "activador/$nombre") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir a Pantalla Activador")
            }
        }
    }
}
