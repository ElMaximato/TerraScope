# Pantallas del reloj (Etapa 1) - obligatorio

Reglas: pantalla redonda pequeña, sesiones de 10-20 segundos, una acción principal por pantalla, texto corto, fondo oscuro.

| # | Pantalla | Contenido | Acción principal | Datos |
|---|---|---|---|---|
| W1 | Inicio | Dos botones grandes: Guardados, Campo. Indicador de conexión con el teléfono | Elegir | - |
| W2 | Guardados | Lista de zonas guardadas (nombre + score pequeño) | Tocar una zona | Teléfono (Data Layer) |
| W3 | Evaluación rápida | Nombre, score + confianza, 2 indicadores clave, fecha de datos | Volver | Teléfono (Data Layer) |
| W4 | Campo - inicio | "Iniciar recorrido" | Iniciar | Local |
| W5 | Sesión activa | Tiempo, número de puntos, botón "Agregar punto", botón "Terminar" | Agregar punto | Local |
| W6 | Agregar punto | Lista de etiquetas (Terreno potencial, Local comercial, Competidor, Acceso vial, Riesgo, Requiere investigación); nota breve opcional (voz o texto corto) | Elegir etiqueta | Local + ubicación puntual |
| W7 | Resumen | Puntos registrados, estado: "Pendiente / Sincronizado" | Listo | Local |

## Reglas de comportamiento
- La ubicación se pide **al agregar un punto** (una lectura puntual), no se rastrea de forma continua.
- Cada punto lleva un identificador único (UUID), fecha, etiqueta, nota y coordenadas; la sincronización es **idempotente** (reenviar el mismo punto no lo duplica).
- Sin teléfono cerca: los puntos quedan en el reloj como "Pendiente" y se envían cuando haya conexión.
- Sin cámara, sin tablas, sin mapa grande.

## Comunicación teléfono ↔ reloj
- **Teléfono → reloj:** lista de zonas guardadas y resumen de cada una.
- **Reloj → teléfono:** sesiones y puntos de campo.
- Mensajes con versión de esquema: `watchlist.updated`, `zone_summary.updated`, `session.started`, `session.ended`, `observation.created`, `sync.requested`.
- El teléfono sube los datos al servidor; el servidor es la fuente central.
- Se prueba con emuladores de teléfono y reloj emparejados desde Android Studio (el procedimiento exacto se documenta en la Etapa 2).

## Opcional (si sobra tiempo)
Alertas al acercarse a una zona guardada (geocercas). Se evalúa después de que lo básico funcione.
