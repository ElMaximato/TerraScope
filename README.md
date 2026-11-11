# TerraScope

Inteligencia territorial para **Somerton, Arizona** (proyecto escolar): una app Android, una app Wear OS y un backend que analizan zonas con datos del Census (ACS) y ayudan a evaluar terrenos y locales.

**Qué hace:** ve las zonas de Somerton en un mapa con indicadores demográficos; calcula un puntaje de inversión según el perfil (bienes raíces, restaurante, comercio, servicios); registra propiedades y dice qué tal está su zona; compara zonas y propiedades; genera reportes (con PDF y resumen opcional por IA); y desde el reloj se marca un lugar con un toque, que llega al teléfono y se guarda en el servidor.

Estado y decisiones: `docs/PROJECT_STATUS.md` · Arquitectura y límites: `docs/ARQUITECTURA_Y_LIMITACIONES.md` · Seguridad: `docs/SEGURIDAD_Y_PRIVACIDAD.md` · Pruebas: `docs/PRUEBAS_E2E.md` · API: `docs/api/openapi.yaml`.

## Estructura
```
android/   Proyecto Android Studio: módulos app (teléfono) y wear (reloj)
backend/   API Node + Express + MySQL
docs/      Investigación, producto, diseño, decisiones, seguridad y guías
scripts/   Utilidades de validación de datos
data/      Muestras de datos
```

## Requisitos
Node 22+, MySQL 8.0.14+ (probado con 8.0.46), JDK 17, Android Studio con SDK 37 y Git.

## 1. Backend
En PowerShell, desde `backend\`:
```powershell
npm install
copy .env.example .env
```
Edita `.env`: `DB_PASSWORD`, `JWT_SECRET` (el archivo trae el comando para generarlo) y, para importar datos, `CENSUS_API_KEY`.

En MySQL crea las bases (una vez):
```sql
CREATE DATABASE IF NOT EXISTS terrascope CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS terrascope_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```
Después:
```powershell
npm run db:setup           # migraciones
npm run db:verify          # funciones espaciales (6 OK)
npm run db:import-somerton # zonas y datos reales de Somerton (Census ACS + TIGERweb); ver docs/REAL_DATA_IMPORT_PLAN.md
npm run db:seed-demo       # (opcional) usuario demo con datos de ejemplo marcados [DEMO]
npm start                  # http://localhost:3000/api/health -> "status":"ok"
```

### Pruebas
```powershell
npm run check       # sintaxis
npm run test:unit   # unitarias (sin MySQL)
npm test            # unitarias + integración (base terrascope_test)
npm run e2e         # extremo a extremo contra el backend en marcha (ver docs/PRUEBAS_E2E.md)
```

## 2. Apps Android
1. Abre la carpeta `android\` con Android Studio y espera el *Gradle sync*.
2. **Mapa:** pon tu clave gratuita de CARTO en `android/app/src/main/assets/map/config.js` (https://carto.com/basemaps/apikey/). Sin clave, el mapa muestra "API KEY REQUIRED".
3. Conecta el teléfono (USB o depuración inalámbrica) o abre un emulador y expón el backend:
   ```powershell
   adb reverse tcp:3000 tcp:3000
   ```
   La app llama a `http://127.0.0.1:3000`. Por seguridad solo se permite HTTP hacia `127.0.0.1`, `localhost` y `10.0.2.2` (`network_security_config.xml`).
4. Ejecuta **app** en el teléfono y **wear** en el reloj.

Cuenta nueva: correo de un proveedor reconocido (Gmail, Outlook, Hotmail, Yahoo, iCloud… o `.edu`/`.edu.mx`, máx. 80 caracteres) y contraseña de 8 a 20 caracteres con al menos una mayúscula y un número. Con `db:seed-demo` puedes entrar con `demo@terrascope.local` / `Demo-TerraScope-2026`.

## 3. Emparejar teléfono y reloj
Ver `docs/setup/emulator-pairing.md`. Con un teléfono real y el reloj emulado: instala "Wear OS by Google" en el teléfono, arranca el emulador del reloj, ejecuta `adb -s <serial> forward tcp:5601 tcp:5601` y en la app Wear OS elige *Pair with emulator*. Ambas apps usan el mismo `applicationId` (`com.terrascope.app`), requisito de la Data Layer.

## 4. IA (opcional)
Para el botón "Reporte IA": crea una clave en Google AI Studio y ponla en `backend/.env` como `GEMINI_API_KEY`. Sin clave todo funciona con el reporte de plantilla. El texto de la IA se descarta si cita cifras que no están en los datos.

## API
Especificación OpenAPI 3: `docs/api/openapi.yaml` (también en http://localhost:3000/api/openapi.yaml; se puede abrir en https://editor.swagger.io).

| Grupo | Rutas |
|---|---|
| Sistema | `GET /api/health`, `GET /api/openapi.yaml` |
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Zonas | `GET /api/zones`, `GET /api/zones/indicators`, `GET /api/zones/:id` |
| Puntaje e inversión | `GET /api/zones/profiles`, `GET /api/zones/profiles/match`, `GET /api/zones/ranking`, `GET /api/zones/:id/score` |
| Reportes | `GET /api/zones/:id/report`, `GET /api/field/analysis` (ambos con `?ai=1` opcional) |
| Propiedades | `GET/POST /api/properties`, `GET/PUT/DELETE /api/properties/:id` |
| Watchlist | `GET/POST /api/watchlist`, `DELETE /api/watchlist/:zoneId` |
| Campo (reloj) | `POST /api/field/sync`, `GET /api/field/sessions`, `GET /api/field/observations`, `PUT/DELETE /api/field/observations/:id` |

Todo, salvo salud, documentación, registro e inicio de sesión, exige `Authorization: Bearer <token>`. Cada usuario solo ve y modifica sus propios datos. Errores con forma única: `{ "error": { "code", "message", "details?" } }`. La sincronización de campo es idempotente: reenviar un lote no duplica nada.

## Datos y licencias
- **Demografía:** U.S. Census Bureau, American Community Survey (dominio público). **Límites de zonas:** Census TIGERweb (dominio público).
- **Mapa:** mapa base de CARTO sobre datos de © OpenStreetMap contributors (ODbL), con la atribución visible en el mapa. Revisa los términos de uso de CARTO si el proyecto se publica o se comercializa.
- **Librerías:** Leaflet (BSD-2), Express y mysql2 (MIT), Jetpack Compose y Wear Compose (Apache-2.0), Google Play services Wearable (términos de Google).
- La foto del letrero de bienvenida del login es una imagen aportada por el autor del proyecto; confirma que puedes usarla antes de publicar la app.
- Los datos de demostración siempre llevan el prefijo "[DEMO]".
