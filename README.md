# Proyecto Peaje IoT - Aplicación Móvil Android (Unidad 2)

* **Estudiante:** Martín Brellenthin Droguett
* **Asignatura:** Aplicaciones Móviles para IoT (TI3042)
* **Sede:** INACAP Temuco

---

## 1. Descripción del Proyecto y Problemática (Paso 1)

Este proyecto continúa el prototipo de **Peaje con RFID** desarrollado en la Unidad 1. En la primera etapa armamos una maqueta con un **Arduino Uno R3**, un lector **RFID RC522** (para validar tarjetas autorizadas), un **sensor ultrasónico HC-SR04** (que detecta cuando el auto está a menos de 10 cm), un **servomotor SG90** para subir y bajar la valla, una pantalla **LCD 16x2 I2C** (que muestra la tarifa de $900) y un módulo **Bluetooth HC-05** conectado en los pines 4 y 5 a 9600 baudios.

**Problemática a resolver en la Unidad 2:**
Se necesitaba desarrollar una aplicación móvil en Android para que el supervisor del peaje pueda monitorear a distancia el estado de la valla, ver la distancia medida por el sensor ultrasónico, revisar los accesos de tarjetas RFID y enviar comandos manuales (`A` para abrir, `C` para cerrar y `D` para medir distancia) por Bluetooth, permitiendo también simular y probar la comunicación entre dos teléfonos Android.

### Herramientas de desarrollo utilizadas
* **Android Studio con Kotlin:** Lenguaje oficial para Android, usando manejo seguro de variables nulas (`?.`, `?:`), colecciones mutables, `data class` y `sealed class`.
* **Jetpack Compose y Material Design 3:** Para construir las pantallas de forma declarativa (`LoginScreen`, `MonitoreoScreen` y `ActivadorScreen`) y manejar la navegación con `NavHost` y `rememberNavController`.

---

## 2. Diseño de la Solución y Comunicación Inalámbrica (Paso 2)

### Elección de Bluetooth
Se eligió **Bluetooth (perfil serial SPP)** porque el prototipo físico del peaje usa un módulo **HC-05** y porque el control de la barrera se realiza a corta distancia desde la caseta de supervisión. Además, al usar una conexión directa punto a punto sin pasar por Internet, es más seguro para un entorno OT.

### Pantallas de la Aplicación
1. **Pantalla de Login (`LoginScreen.kt`):**
   * Pide usuario y contraseña antes de dejar entrar al panel del peaje.
   * **Usuario de prueba:** `supervisor` | **Contraseña:** `peaje2026`.
2. **Pantalla de Monitoreo (`MonitoreoScreen.kt`):**
   * Permite buscar y conectarse por Bluetooth al módulo HC-05 del Arduino o activar el **Modo App 2** para conectarse con otro celular Android.
   * Muestra en tiempo real si la valla está abierta o cerrada, los centímetros del sensor HC-SR04, el estado de la última tarjeta RFID y una lista (`LazyColumn`) con todos los logs recibidos y un `Checkbox` para filtrarlos.
3. **Pantalla Activador (`ActivadorScreen.kt`):**
   * **Switch:** Abre (`A`) o cierra (`C`) la valla directamente.
   * **RadioButtons:** Permite elegir y enviar las órdenes `Abrir valla (A)`, `Cerrar valla (C)` o `Medir distancia (D)`.
   * **Slider:** Permite ajustar la distancia en centímetros (de 2 a 100 cm) y enviarla al otro teléfono durante las pruebas.
   * **TextField (EditText):** Campo de texto para escribir y enviar comandos manuales al dispositivo conectado.

---

## 3. Medidas de Seguridad ISO y Lineamientos OT (Paso 3)

Para cumplir con la norma **ISO/IEC 27001 / ISO 27400** y las buenas prácticas de seguridad en **OT**:

* **Control de acceso y contraseña encriptada (ISO 27001):** La aplicación no guarda la contraseña en texto plano. En `BluetoothController.kt` se aplica un hash **SHA-256** para comparar la clave ingresada y, si alguien falla 3 veces seguidas en el Login, se bloquea el botón de ingreso.
* **Permisos mínimos en Android:** En `AndroidManifest.xml` se desactivó el respaldo automático de datos (`allowBackup="false"`) y se piden en tiempo de ejecución solo los permisos `BLUETOOTH_CONNECT` y `BLUETOOTH_SCAN` sin acceder a la ubicación GPS del usuario (`neverForLocation`).
* **Validación de comandos (Seguridad OT):** Para evitar que se envíen datos erróneos que afecten al servomotor de la maqueta, la función `enviarDatos()` revisa que los comandos manuales estén dentro de la lista permitida (`A`, `C` o `D`), igual que la validación programada en el Arduino de la Unidad 1.
* **Protección física del actuador (OT):** En el peaje, el servomotor se desconecta (`servo.detach()`) después de moverse para evitar interferencias con el puerto Bluetooth, y la barrera tiene un cierre automático de seguridad a los 8 segundos si el auto no cruza.

---

## 4. Pruebas de Interconexión y Escenarios de Uso

Se probó la estabilidad de la aplicación en 4 escenarios:

| Escenario de Prueba | Qué se hizo | Resultado obtenido |
| :--- | :--- | :--- |
| **1. Conexión entre 2 teléfonos Android** | Se activó el "Modo App 2" en un celular y desde el otro celular se conectó por Bluetooth enviando `A` y `C` con el Switch y los RadioButtons. | El segundo teléfono recibió los comandos al instante, cambió su estado y respondió `[VALLA] Abierta (manual)` y `[VALLA] Cerrada (manual)`. |
| **2. Envío de distancia con Slider / Sensor** | Se movió el Slider de distancia y se envió el comando `D` para consultar los centímetros. | La pantalla de Monitoreo actualizó el valor de distancia en centímetros sin trabar la aplicación. |
| **3. Envío de comando no válido** | Se escribió una letra cualquiera (por ejemplo `X`) en el TextField del Activador y se presionó Enviar. | El filtro OT bloqueó el envío mostrando `[CMD] Comando no valido. Use: A, C, D` en los logs. |
| **4. Desconexión de Bluetooth** | Se apagó el Bluetooth de uno de los teléfonos mientras estaban conectados. | La app capturó el error (`IOException`), cerró el socket limpiamente y mostró "Se perdio la conexion" sin cerrarse de golpe. |




