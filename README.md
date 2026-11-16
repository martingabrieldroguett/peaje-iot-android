k# Proyecto Peaje IoT - Aplicación Móvil Android (Unidad 2)

* **Estudiante:** Martín Brellenthin Droguett
* **Asignatura:** Aplicaciones Móviles para IoT (TI3042)
* **Sede:** INACAP Temuco

---

## 1. Historias de Usuario (Formato Connextra)
* **HU-01 (Autenticación):** Como Administrador del Sistema, quiero iniciar sesión con credenciales cifradas mediante hash SHA-256 para restringir el acceso no autorizado al panel de control del peaje.
* **HU-02 (Monitoreo Supervisor Wi-Fi):** Como Supervisor, quiero consultar el estado de la valla y la telemetría del sensor ultrasónico mediante red Wi-Fi para supervisar las operaciones a distancia de forma segura.
* **HU-03 (Control Operador Bluetooth):** Como Operador en Caseta, quiero accionar la valla localmente mediante Bluetooth para agilizar el paso vehicular en la pista.

## 2. Arquitectura de Red y Modelo Híbrido
* **Operador Local (Caseta):** Emplea conectividad **Bluetooth** de corto alcance para el control directo del hardware en pista (Arduino Uno R3, lector RFID MFRC522 y servomotor SG90).
* **Administrador / Supervisor (Remoto):** Emplea conectividad **Wi-Fi / Red Local** para la supervisión administrativa a distancia, cumpliendo con los lineamientos de separación de entornos industriales OT.

## 3. Seguridad ISO/IEC 27001 y Android Keystore
* **Autenticación Segura:** Las credenciales del supervisor son validadas mediante un hash criptográfico **SHA-256 con Salt**, bloqueando el acceso tras 3 intentos fallidos.
* **Android Keystore:** Se integra conceptualmente el almacenamiento en el contenedor criptográfico seguro del sistema operativo Android para proteger llaves y credenciales.
* **Validación de Comandos OT:** El sistema filtra las tramas salientes y entrantes mediante una lista blanca de comandos permitidos (`A`, `C`, `D`), evitando errores de operación en el actuador físico.
