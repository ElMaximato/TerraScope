# TerraScope — Revisión de seguridad y privacidad (Etapa 10A)

Alcance: backend (Express + MySQL), app Android y app Wear OS. Es una revisión de código y configuración hecha por el equipo de desarrollo, no una auditoría profesional.

## Qué se revisó y qué se encontró

| Área | Resultado |
|---|---|
| Autenticación | Bien. Contraseñas con scrypt y sal aleatoria; comparación de tiempo constante; login con el mismo mensaje y tiempo parecido exista o no el correo; JWT HS256 con algoritmo fijado, expiración (7 días) y secreto de mínimo 32 caracteres exigido al arrancar. |
| Autorización | Bien. Todas las rutas de zonas, propiedades, lista de seguimiento y campo exigen sesión. Toda consulta de propiedades, seguimiento y puntos de campo filtra por `user_id` (se revisó cada SQL). Un recurso ajeno responde 404, no 403. |
| Inyección SQL | Bien. Todo con parámetros (`?`); lo único interpolado son fragmentos fijos del código y límites numéricos validados. |
| Validación de entradas | Bien. Ids numéricos o UUID validados, tamaño de cuerpo limitado a 100 KB, máximo 100 elementos por sincronización, etiquetas contra lista permitida. |
| Errores | Bien. Los 500 no muestran detalles internos; las contraseñas no se registran en el log. |
| Límite de peticiones | Había solo para login/registro. **Corregido:** ahora también hay límite por usuario (10 cada 10 min, `AI_RATE_LIMIT_MAX`) en las llamadas con IA, para cuidar la cuota del proveedor. |
| Cabeceras HTTP | Faltaban. **Corregido:** `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control: no-store` en la API y sin `X-Powered-By`. |
| CORS | Estaba abierto a cualquier origen. **Corregido:** desactivado por defecto (la app móvil no lo usa); se activa con `CORS_ORIGIN`. |
| Respaldo de Android | `allowBackup=true` podía copiar la sesión (token) a la nube. **Corregido:** respaldo y transferencia desactivados. |
| Tráfico HTTP en claro | Estaba permitido para todo destino. **Corregido:** solo hacia `127.0.0.1`, `localhost` y `10.0.2.2`; todo lo demás exige HTTPS. |
| Permisos | Teléfono: solo `INTERNET`. Reloj: ubicación fina/gruesa (necesaria para marcar puntos) y `WAKE_LOCK`. La ubicación solo se lee cuando el usuario toca "Marcar aquí". |
| Sincronización reloj↔teléfono | El Data Layer de Google solo entrega datos entre apps con el mismo `applicationId` y firma. |

## Riesgos que se aceptan (y por qué)

1. **Sesión en el teléfono sin cifrar adicional.** El token está en DataStore, dentro del almacenamiento privado de la app. Para un proyecto escolar es aceptable; en producción se usaría el Keystore de Android.
2. **Sin HTTPS en el backend.** Solo corre en la computadora del desarrollador, accesible por `adb reverse`. Para desplegarlo en internet hace falta un proxy con TLS.
3. **El JWT no se puede revocar** antes de expirar (7 días) y no hay renovación.
4. **Límites de peticiones en memoria:** se reinician al reiniciar el servidor y no sirven con varias instancias.
5. **Clave de CARTO dentro de la app** (`assets/map/config.js`). Cualquiera que descomprima el APK la puede leer. Es una clave de mapas gratuita; conviene restringirla por cuota en el panel de CARTO.
6. **Claves por defecto de la base:** si `.env` no define `DB_USER`/`DB_PASSWORD`, el backend usa `root` sin contraseña. Solo para desarrollo local; en cualquier otro entorno, definirlas.
7. **No hay endpoint para borrar la cuenta.** Se pueden borrar los puntos y propiedades uno por uno, pero no la cuenta completa. Pendiente como trabajo futuro.

## Privacidad

- Se guardan ubicaciones exactas de los puntos marcados y de las propiedades registradas, ligadas al usuario. Solo el propio usuario las puede ver, editar o borrar.
- La IA opcional recibe únicamente cifras públicas del Census y los puntajes y nombres de zona del reporte; no recibe correo, contraseña ni coordenadas exactas. En el análisis de ubicaciones recibe los **nombres** que el usuario les puso a sus puntos y propiedades; conviene no poner datos personales en esos nombres.
- La clave de la IA vive solo en el `.env` del backend, nunca en la app.

## Pruebas automáticas relacionadas

- `backend/tests/unit/security.test.js`: sin sesión, cada ruta protegida responde 401; tokens falsificados, expirados o con `alg: none` se rechazan; cabeceras de seguridad; CORS desactivado; cuerpos grandes y JSON roto.
- `backend/tests/unit/aiRateLimit.test.js`: el límite de IA es por usuario.
- No se pueden probar de forma automática (requieren MySQL): que un usuario no vea datos de otro. Se verificó leyendo cada consulta; se recomienda una prueba manual con dos cuentas.
