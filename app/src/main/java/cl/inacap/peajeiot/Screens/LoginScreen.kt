package cl.inacap.peajeiot.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cl.inacap.peajeiot.BluetoothController

@Composable
fun LoginScreen(navController: NavHostController) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var mensajeError by rememberSaveable { mutableStateOf("") }
    var intentos by rememberSaveable { mutableIntStateOf(0) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sistema de Peaje IoT",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ingreso Supervisor (Tarifa: $900)")

            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = usuario,
                onValueChange = { usuario = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (mensajeError.isNotEmpty()) {
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (BluetoothController.validarLogin(usuario, clave)) {
                        mensajeError = ""
                        intentos = 0
                        navController.navigate(route = "monitoreo/$usuario")
                    } else {
                        intentos++
                        mensajeError = "Usuario o contraseña incorrectos ($intentos/3)"
                    }
                },
                enabled = intentos < 3,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (intentos < 3) "Ingresar" else "Acceso Bloqueado")
            }

            // Boton para reiniciar intentos durante la prueba en clases
            if (intentos >= 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    intentos = 0
                    mensajeError = ""
                }) {
                    Text("Reiniciar intentos")
                }
            }
        }
    }
}
