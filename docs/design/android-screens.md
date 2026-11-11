# Pantallas del teléfono (Etapa 1)

Cada pantalla tiene una función y una fuente de datos. "Endpoint" es una propuesta para la Etapa 3.

| # | Pantalla | Contenido (poco y esencial) | Fuente de datos | Endpoint propuesto |
|---|---|---|---|---|
| A1 | Bienvenida | Logo, frase corta, "Comenzar" | - | - |
| A2 | Registro / Inicio de sesión | Correo, contraseña | Cuenta propia | `POST /auth/register`, `POST /auth/login` |
| A3 | Inicio | Saludo, tarjeta "Somerton hoy" (población, ingreso mediano, con año), zonas guardadas (2-3), última sesión de campo | Census; BD del usuario | `GET /city/summary`, `GET /watchlist` |
| A4 | Mapa | Mapa de Somerton con zonas y chips de capa (Zonas, Negocios, Vías); hoja inferior con la zona tocada | Zonas (Census/TIGER), OSM | `GET /zones`, `GET /places` |
| A5 | Ficha de zona | Nombre, anillo de score + confianza, 4 indicadores clave, favorables/riesgos (máx. 3 c/u), "Guardar", enlace a fuentes | Census, OSM, cálculo del score | `GET /zones/{id}` |
| A6 | Desglose del score | Factores, peso y aporte; datos faltantes; versión de metodología; aviso orientativo | Cálculo propio | `GET /zones/{id}/score` |
| A7 | Guardados - Zonas | Lista de zonas guardadas | BD del usuario | `GET /watchlist` |
| A8 | Guardados - Propiedades | Lista con precio solicitado y zona | BD del usuario | `GET /properties` |
| A9 | Formulario de propiedad | Nombre, ubicación (pin en mapa), tipo, superficie, precio, notas | Captura del usuario | `POST/PUT/DELETE /properties` |
| A10 | Campo | Sesiones del reloj, cantidad de puntos, estado de sincronización | Reloj → servidor | `GET /field-sessions` |
| A11 | Detalle de sesión | Puntos con etiqueta, nota y hora; mapa pequeño | Reloj → servidor | `GET /field-sessions/{id}` |
| A12 | Perfil y ajustes | Nombre, unidades (mi/km), permisos de ubicación, cerrar sesión | Cuenta | `GET/PATCH /me` |

## Fuera de esta versión (se diseñarán si sobra tiempo)
Comparación de zonas, reportes en PDF, filtros avanzados, perfiles de inversión adicionales, alertas de cambios.

## Equivalencia con el diseño base de Figma
El Figma base trae pantallas como inicio, mapa, oportunidades, ficha de zona, propiedades, comparación, watchlist, reportes, campo, perfil, ajustes y reloj, con tema oscuro y cifras de demostración. Se reutiliza **solo la idea de la estructura**; se reconstruye todo en Compose con el tema claro de este documento y datos reales.

## Decisión pendiente: proveedor de mapa
Propuesta: Google Maps SDK con `maps-compose` (el más directo en Android Studio; requiere clave de API). Alternativa sin clave: MapLibre/OpenStreetMap. Se decide en la Etapa 2 tras revisar costos y cuotas.
