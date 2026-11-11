# TerraScope - Contexto de traspaso (handoff)

**Fecha de corte:** 2026-11-06
**Uso:** pégalo en una conversación nueva junto con `TerraScope_Especificacion_y_Plan_de_Desarrollo.md` (la especificación base). Este documento resume lo ya hecho, las decisiones tomadas y dónde continuar.

---

## 1. Qué es el proyecto
TerraScope es una app de inteligencia territorial/inmobiliaria para **Somerton, Arizona**: ayuda a evaluar zonas para invertir o abrir un negocio, con datos trazables (fuente, año, tipo de dato). Tiene app móvil Android, app **Wear OS (obligatoria)** y un backend.

**Es para una actividad/examen escolar**, así que el alcance es reducido y debe funcionar rápido y sin complicaciones.

- Carpeta local del usuario: `C:\Users\ferba\Documents\ProyectosUT\inversiones_somerton`
- Usuario: Fer (hispanohablante). Responder en español.

## 2. Reglas de trabajo acordadas
1. **Por pasos (etapas)**; no avanzar a la siguiente sin aprobación del usuario.
2. **Cada entrega es un .zip con SOLO archivos nuevos o modificados**, con rutas relativas a `inversiones_somerton\` (se descomprime encima). Nunca el proyecto completo (ahorra tokens).
3. Cada entrega indica: archivos nuevos, archivos modificados, cómo ejecutar/validar, y qué se probó de verdad. **No afirmar que algo funciona si no se ejecutó.**
4. Distinguir: implementado y probado / implementado sin probar / diseñado / bloqueado.
5. No inventar datos. Los datos de demostración deben estar claramente marcados como tales.
6. Mantener `docs/PROJECT_STATUS.md` actualizado en cada paso.
7. El usuario prueba todo desde **Android Studio** (teléfono y reloj a la vez, con emuladores). Tuvo problemas mezclando lenguajes en un proyecto anterior: mantener la solución simple.

## 3. Decisiones tomadas
| Tema | Decisión |
|---|---|
| Base de datos | **MySQL** (con MySQL Workbench). Se descartó PostgreSQL/PostGIS por preferencia del usuario. Usar tipos y funciones espaciales de MySQL 8 (`POINT`, `POLYGON`, `ST_Distance_Sphere`, `ST_Contains`, índices espaciales). **Por verificar en la Etapa 2** que alcancen. |
| App móvil + reloj | **Un solo proyecto de Android Studio**, módulos `app` (teléfono) y `wear` (reloj), **Kotlin + Jetpack Compose**. |
| Backend | **Node.js + Express + MySQL**, corre aparte (terminal). Las apps lo consumen por HTTP. |
| Reloj | **Obligatorio**. Se conecta al teléfono; el teléfono sube al backend. |
| Diseño | Minimalista, claro, premium, poca información importante. Tema **claro** en teléfono; fondo oscuro en reloj. |
| Perfil de análisis | Inversión comercial / site selection. |
| Unidad de análisis | **Census tracts** (los block groups son demasiado ruidosos). Falta obtener geometrías TIGER. |

> El stack (Kotlin + Node) lo propuso el asistente y el usuario lo delegó; confirmarlo al cerrar la Etapa 1 si el usuario tiene dudas.

## 4. Estado de las etapas
| Etapa | Estado |
|---|---|
| 0 - Descubrimiento y viabilidad | **Cerrada** |
| 0.5 - Validación de datos reales | **Cerrada** (ejecutada por el usuario) |
| 1 - Flujos y UX | **Entregada, en espera de aprobación del usuario** |
| 2 - Repositorio y proyecto base | **Siguiente** (no iniciada) |
| 3 a 10 | Pendientes (ver la especificación, sección 14) |

**Dónde nos quedamos:** el usuario dijo "Perfecto, continúa" tras la Etapa 1. Falta empezar la **Etapa 2**.

### Zips entregados hasta ahora
1. `etapa0_descubrimiento.zip` - investigación de fuentes y MVP.
2. `etapa0b_validacion_datos.zip` - script `scripts/validate-data.mjs` y README.
3. `etapa0b_fix_diagnostico.zip` - script con diagnóstico de errores del Census.
4. `etapa0b_resultados.zip` - resultados reales documentados.
5. `etapa1_flujos_ux.zip` - flujos, sistema de diseño y pantallas.

### Archivos que ya existen en `inversiones_somerton\`
```
docs/
  PROJECT_STATUS.md
  research/  data-sources.md, data-availability-matrix.md, validation-results.md
  product/   mvp-scope.md, risks-and-decisions.md, user-flows.md
  design/    design-system.md, android-screens.md, wear-os-screens.md
scripts/     validate-data.mjs, README.md
data/samples/  (generados por el script en la laptop del usuario: validation-report.json, census_*.json, osm_pois_somerton.json)
```
El zip de Figma que dio el usuario (React web, tema oscuro, datos de demostración) es **solo referencia visual**; no se reutiliza código.

## 5. Hallazgos de datos (verificados con llamadas reales)
**Census ACS 5 años 2024 - Somerton city, AZ (place 04-68080):**
población 14,574 (±19) · ingreso mediano del hogar $74,015 (±$11,292) · edad mediana 31.3 · hogares 4,964.

- La API del Census **exige clave** (gratuita). El script la lee de la variable de entorno `CENSUS_API_KEY`. **No pegar la clave en el chat.** En PowerShell: `$env:CENSUS_API_KEY="..."` y luego `node scripts/validate-data.mjs` (Node 18+).
- **Block groups:** unos 165 en el condado de Yuma, con márgenes de error enormes, varios sin ingreso y valores atípicos. No sirven como cifra puntual, y no se sabe cuáles caen en Somerton sin geometrías.
- **Tracts:** la consulta falló una vez con "Invalid Key" (probablemente transitorio); repetir.
- **OpenStreetMap (caja aprox. 32.57,-114.75 a 32.63,-114.67):** 68 lugares (40 con nombre; 24 son estacionamientos), 53 vías principales con nombre. Cobertura de negocios **escasa**: complementar con captura manual o datos de demostración marcados.
- **Overpass** puede dar 502 transitorio; el script usa un servidor de respaldo.
- **Zillow:** no hay API pública (retirada en 2021); no habrá precios de mercado. Las propiedades se capturan a mano ("Aportado por ti").
- **Zonificación/parcelas:** el código de zonificación de Somerton existe en PDF; no se confirmó capa GIS descargable. La tribu Cocopah tiene su propia oficina de planeación (jurisdicción distinta). Fuera del MVP.
- ODbL: atribuir "© OpenStreetMap contributors".

## 6. Diseño (resumen)
- Colores: fondo `#F6F8F7`, superficie `#FFFFFF`, primario verde `#0F4D3A`, acento dorado `#E3AE55`, texto `#14211C`. Reloj: fondo `#0B1512`.
- Tipografía Inter; esquinas redondeadas; sin emojis; Material Symbols.
- **Teléfono (4 pestañas):** Inicio, Mapa, Guardados (Zonas/Propiedades), Campo; perfil desde el avatar. 12 pantallas (A1-A12).
- **Reloj:** Inicio, Guardados, Evaluación rápida, Campo (iniciar, sesión activa, agregar punto con etiqueta y nota, resumen). 7 pantallas (W1-W7). Ubicación solo al agregar un punto; sincronización idempotente con UUID por punto.
- El score se muestra **siempre con su nivel de confianza aparte**; si faltan datos, "Datos insuficientes" en vez de un número. Cada indicador lleva fuente, año y tipo (Oficial / Estimación / Derivado / Aportado por ti).
- Detalle completo en `docs/design/` y `docs/product/user-flows.md`.

## 7. Decisiones pendientes
1. **Proveedor de mapas:** propuesta Google Maps SDK + `maps-compose` (requiere clave); alternativa sin clave: MapLibre/OpenStreetMap. Cerrar en la Etapa 2.
2. Autenticación (propuesta simple: correo + contraseña con hash en el backend, token JWT).
3. Hosting del backend/BD (para el examen puede bastar local).
4. Almacenamiento de imágenes (probablemente fuera del alcance).
5. Geometrías TIGER de tracts para ubicar Somerton (Etapa 4).
6. Método y ponderaciones del score (Etapa 6).
7. Geocercas en el reloj (opcional).
8. IA (opcional; las funciones centrales no deben depender de ella).

## 8. Siguiente paso: Etapa 2 (repositorio y proyecto base)
Objetivo: base técnica que compile y arranque, sin implementar el producto.
1. Preguntar al usuario las versiones instaladas (Android Studio, JDK, Node, MySQL/XAMPP) y documentarlas.
2. Estructura propuesta: `android/` (proyecto único con módulos `app` y `wear`), `backend/` (Node + Express), `docs/`, `scripts/`, `data/samples/`.
3. Proyecto Android Kotlin + Compose con los dos módulos compilando.
4. Backend Express con endpoint de salud y conexión a MySQL; `.env.example`, `.gitignore`.
5. Esquema MySQL mínimo y verificación de funciones espaciales.
6. Decidir proveedor de mapas.
7. Documentar cómo **emparejar los emuladores de teléfono y reloj** en Android Studio.
8. Compilación mínima de ambos módulos y arranque del backend. README.
9. Entregar un zip **solo con lo nuevo**, actualizar `PROJECT_STATUS.md` y esperar aprobación.

## 9. Notas para la IA que continúe
- Leer primero este documento, la especificación y `docs/PROJECT_STATUS.md`.
- Seguir las reglas de la sección 2 y la sección 17 de la especificación.
- Presentar opciones con ventajas, desventajas y recomendación antes de decisiones importantes.
- Al final de cada paso, entregar el informe con: objetivo, archivos nuevos y modificados, decisiones, pruebas ejecutadas y resultado, problemas pendientes, cómo validar y siguiente tarea propuesta.
