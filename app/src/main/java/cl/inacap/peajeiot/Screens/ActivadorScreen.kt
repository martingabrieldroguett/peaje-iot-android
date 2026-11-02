package cl.inacap.peajeiot.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cl.inacap.peajeiot.BluetoothController

@Composable
fun ActivadorScreen(navController: NavHostController, nombre: String) {
    // Opciones para los RadioButton (comandos A, C y D del Arduino)
    val opciones = listOf("Abrir valla (A)", "Cerrar valla (C)", "Medir distancia (D)")
    var seleccion by rememberSaveable { mutableStateOf(opciones[0]) }

    // Variables para el Slider y el TextField
    var valorSlider by rememberSaveable { mutableFloatStateOf(10f) }
    var textoComando by rememberSaveable { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Activador de Peaje - $nombre",
                style = MaterialTheme.typography.headlineSmall
            )

            // 1. Switch para abrir o cerrar la valla
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Control Manual de Valla (Switch)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = BluetoothController.switchAbierto,
                            onCheckedChange = { activo ->
                                BluetoothController.switchAbierto = activo
                                if (activo) {
                                    BluetoothController.enviarDatos("A")
                                } else {
                                    BluetoothController.enviarDatos("C")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (BluetoothController.switchAbierto) "Abierta (A)" else "Cerrada (C)")
                    }
                }
            }

            // 2. RadioButtons para enviar comandos rapidos
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Comandos del Supervisor (RadioButton):")
                    opciones.forEach { opcion ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (opcion == seleccion),
                                onClick = {
                                    seleccion = opcion
                                    when (opcion) {
                                        opciones[0] -> BluetoothController.enviarDatos("A")
                                        opciones[1] -> BluetoothController.enviarDatos("C")
                                        opciones[2] -> BluetoothController.enviarDatos("D")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opcion)
                        }
                    }
                }
            }

            // 3. Slider para simular o ajustar distancia en cm
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Distancia Sensor Ultrasonico: ${valorSlider.toInt()} cm")
                    Slider(
                        value = valorSlider,
                        onValueChange = {
                            valorSlider = it
                            BluetoothController.distanciaAuto = it.toInt()
                        },
                        valueRange = 2f..100f
                    )
                    Button(
                        onClick = {
                            BluetoothController.enviarDatos(
                                "Distancia: ${valorSlider.toInt()} cm",
                                revisarComando = false
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enviar Distancia al otro dispositivo")
                    }
                }
            }

            // 4. TextField (EditText) para escribir un comando manual
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Enviar comando manual (A, C o D):")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = textoComando,
                            onValueChange = { textoComando = it },
                            label = { Text("Comando") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            BluetoothController.enviarDatos(textoComando)
                            textoComando = ""
                        }) {
                            Text("Enviar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate(route = "monitoreo/$nombre") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver a Monitoreo")
            }
        }
    }
}
