# Flujos de usuario y navegación (Etapa 1)

Fecha: 2026-11-06 · **Requisito de la actividad: app móvil conectada con Wear OS (el reloj es obligatorio).**

## 1. Usuario y casos de uso
**Usuario:** inversionista o emprendedor que evalúa dónde abrir un negocio en Somerton.

| # | Caso de uso | Dispositivo |
|---|---|---|
| CU1 | Crear cuenta e iniciar sesión | Teléfono |
| CU2 | Ver el mapa de Somerton y tocar una zona | Teléfono |
| CU3 | Leer la ficha de la zona: indicadores, score, confianza, fuentes | Teléfono |
| CU4 | Guardar una zona en Guardados | Teléfono |
| CU5 | Registrar una propiedad manualmente y ligarla a su zona | Teléfono |
| CU6 | Ver zonas guardadas y su resumen rápido | Reloj |
| CU7 | Iniciar una sesión de campo, marcar puntos con etiqueta y nota breve, terminar la sesión | Reloj |
| CU8 | Ver en el teléfono lo registrado por el reloj, con estado de sincronización | Teléfono |

## 2. Mapa de navegación: teléfono
Barra inferior con 4 pestañas (estilo de la referencia):

```
Login / Registro
   └─ Inicio ── Mapa ── Guardados ── Campo
        │         │          │          │
        │         │          │          └─ Detalle de sesión (puntos, notas)
        │         │          ├─ Zonas guardadas ─┐
        │         │          └─ Propiedades ──── Formulario de propiedad
        │         └─ Ficha de zona (hoja inferior → pantalla completa)
        │                └─ Desglose del score
        └─ Perfil / Ajustes (desde el avatar, arriba a la derecha)
```
- **Inicio:** resumen corto (zonas guardadas, última sesión de campo, estado de datos) y acceso al mapa.
- **Mapa:** zonas de Somerton como polígonos; tocar una zona abre la ficha.
- **Guardados:** dos pestañas internas, Zonas y Propiedades.
- **Campo:** lista de sesiones del reloj y estado de sincronización.

## 3. Mapa de navegación: reloj
```
Inicio del reloj
 ├─ Guardados (lista de zonas) ── Evaluación rápida de la zona
 └─ Campo ── Iniciar sesión ── Sesión activa ── Agregar punto (etiqueta + nota) ── Terminar sesión ── Resumen
```

## 4. Flujos principales
**F1 - Explorar y guardar una zona (teléfono)**
1. Abrir Mapa → 2. tocar una zona → 3. ver la hoja con nombre, score y confianza → 4. "Ver ficha" → 5. revisar indicadores y fuentes → 6. "Guardar".
Estados: cargando (esqueleto), sin datos suficientes (se muestra "Datos insuficientes" en vez de score), error con reintento.

**F2 - Registrar propiedad (teléfono)**
Guardados → Propiedades → "+" → formulario (nombre, ubicación en mapa, tipo, superficie, precio solicitado, notas) → guardar. Se marca "Aportado por ti". Se asocia a su zona automáticamente.

**F3 - Recorrido de campo (reloj → teléfono)**
Reloj: Campo → Iniciar → (por cada punto) Agregar punto → elegir etiqueta → nota breve opcional → ... → Terminar.
Se guarda en el reloj con un identificador único y se envía al teléfono; el teléfono lo sube al servidor. En el teléfono: Campo → ver sesión.
Sin conexión: el punto queda "pendiente" y se sincroniza después, sin duplicarse.

**F4 - Consulta rápida (reloj)**
Reloj: Guardados → elegir zona → ver score, 2 indicadores clave, fecha de datos.

## 5. Estados globales (todas las pantallas)
| Estado | Comportamiento |
|---|---|
| Cargando | Esqueleto gris suave, nunca pantalla en blanco |
| Vacío | Texto corto y una acción ("Aún no guardas zonas. Abre el mapa") |
| Error | Mensaje breve + botón Reintentar |
| Sin conexión | Banda discreta; se muestran datos locales si existen |
| Pendiente de sincronizar | Punto dorado junto al elemento |
| Dato insuficiente | Se indica qué falta; no se inventa un score |

## 6. Reglas de contenido (de la especificación)
- Cada indicador muestra valor, fuente, año y tipo (Oficial / Estimación / Derivado / Aportado por ti).
- El score nunca va solo: siempre junto a "Confianza" y al aviso "Análisis orientativo, no garantiza resultados".
- Datos ACS con margen de error visible; OpenStreetMap con atribución "© OpenStreetMap contributors".
- Los negocios cercanos se muestran con la nota "Cobertura limitada" (hallazgo de la Etapa 0.5).
