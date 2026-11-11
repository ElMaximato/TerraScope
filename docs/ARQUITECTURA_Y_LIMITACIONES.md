# TerraScope — Arquitectura, decisiones, límites y trabajo futuro

## Vista general

```
 Reloj (Wear OS)            Teléfono (Android)                 Backend (Node + Express)        MySQL 8
 ───────────────            ──────────────────                 ────────────────────────        ───────
 "Marcar aquí" ──Data Layer──► Servicio de sincronización ──HTTP+JWT──► /api/field/sync ───────► zonas, indicadores,
 (GPS, etiqueta)  (/field/points)   sube y confirma (ack)                /api/zones/...          propiedades, puntos,
 Zonas guardadas ◄─Data Layer── Publica zonas + puntajes                 /api/properties         usuarios, lista de seguimiento
                  (/zones/saved)  (/zones/saved)                         /api/field/analysis
                                  Mapa (Leaflet en WebView)                    │
                                  Reportes, PDF, comparadores                  └─► IA opcional (Gemini), con validación
```

- **App del teléfono:** Kotlin + Jetpack Compose (Material 3). El mapa es Leaflet dentro de un WebView, con puente Kotlin↔JS. Las pantallas usan ViewModels con `StateFlow`.
- **App del reloj:** Wear Compose. Funciona sin conexión: guarda los puntos localmente y los envía cuando el teléfono está disponible.
- **Backend:** Express, `mysql2` con consultas parametrizadas, JWT (HS256, 7 días), scrypt para contraseñas. MySQL con tipos espaciales (SRID 4326).
- **Comunicación reloj↔teléfono:** Data Layer de Google (`DataClient` para datos, `MessageClient` para la confirmación). Requiere el mismo `applicationId` y firma en ambas apps.

## Decisiones principales

| Decisión | Motivo |
|---|---|
| Puntaje relativo y solo demográfico | Es lo que los datos del Census permiten defender; no hay datos fiables de precios, competencia ni tráfico. Se muestra la limitación en la app. |
| Normalización mín-máx entre las 5 zonas × pesos por perfil | Simple, explicable y reproducible (mismos datos → mismo resultado). Hay pruebas unitarias. |
| Confianza por coeficiente de variación del margen de error | Los datos del ACS son estimaciones; un dato con mucho margen no debe parecer firme. |
| Reporte por plantilla; IA solo como complemento | El reporte funciona sin proveedor externo; la IA nunca es la fuente de las cifras y su texto se descarta si cita números ajenos a los datos. |
| Leaflet + CARTO en vez de MapLibre/OSM directo | Funciona en WebView sin SDK nativo. CARTO exige una clave gratuita. |
| Sincronización idempotente con UUID generados en el reloj | Con red inestable se puede reintentar sin duplicar. |
| Un solo `applicationId` para teléfono y reloj | Requisito de la Data Layer. |

## Limitaciones conocidas

1. **El puntaje es de la zona, no del predio.** Dos ubicaciones en la misma zona censal empatan. No considera competencia, precios de venta o renta, tráfico, uso de suelo ni riesgos.
2. **Solo 5 zonas censales** (tracts) en Somerton: el ranking compara pocas unidades y un cambio pequeño mueve las posiciones.
3. **Datos del ACS 5 años:** estimaciones con margen de error; pueden estar desfasadas.
4. **La densidad se calcula contra el área total** de la zona (incluye terreno agrícola), por lo que resulta baja.
5. **GPS del reloj:** la precisión baja en interiores y en movimiento (por ejemplo, manejando). El punto marcado puede quedar a decenas de metros.
6. **Sincronización:** el reloj necesita el teléfono cerca (Bluetooth o Wi-Fi) y la app del teléfono con sesión iniciada. Si se borra en el teléfono un punto que el reloj aún muestra como "por enviar", puede reaparecer.
7. **Sin cuenta recuperable:** no hay "olvidé mi contraseña" ni borrado de cuenta; el JWT no se puede revocar antes de expirar.
8. **IA opcional:** depende de un proveedor externo (nivel gratuito, con límites y picos de demanda). Cuando falla, se muestra el reporte de plantilla.
9. **Backend local:** corre en la computadora del desarrollador y se alcanza con `adb reverse`; no está desplegado ni usa HTTPS.
10. **PDF de texto simple**, sin gráficas ni logo.
11. **Prueba sin compilar en el entorno de desarrollo asistido:** parte del código Android se escribió sin poder compilarlo ahí; se validó en los dispositivos del autor.

## Trabajo futuro

- Incorporar datos de precios y competencia (por ejemplo, listados y negocios de OpenStreetMap) para puntajes por predio.
- Más zonas (bloques censales) y más ciudades.
- Despliegue con HTTPS, renovación de sesión y borrado de cuenta.
- Gráficas y logo en el PDF; modo oscuro; pulido final del diseño.
- Pruebas automatizadas de interfaz (Compose) y de la sincronización reloj↔teléfono.

## Dependencias de terceros y licencias

Ver la sección "Datos y licencias" del `README.md`.
