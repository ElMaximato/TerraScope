# Mapa en blanco en el teléfono físico: qué se cambió y cómo diagnosticarlo

## Síntoma
En el Samsung (Wi-Fi debugging) la pestaña Mapa mostraba la tarjeta de arriba con "5 zonas del Census" y la leyenda con valores reales, pero el área del mapa quedaba lisa: sin mapa base, sin polígonos y sin etiquetas.

## Qué se concluyó
- Los datos **sí llegan** de la API (la leyenda muestra 2,163 a 5,204).
- El área del mapa tiene el color de fondo del propio WebView (`#EEF2F1`): el WebView se dibuja, pero no recibe o no muestra nada.
- El JavaScript de `map.html` se probó con los límites reales de TIGER del proyecto (5 tracts + la ciudad): dibuja 6 polígonos y 5 etiquetas sin errores. El fallo está en cómo la app se comunica con el WebView o en los mosaicos, no en los datos.
 - **Estado final comprobado el 2026-11-07:** el mapa ya carga en el teléfono/emulador después de aplicar las correcciones descritas abajo y usar el proveedor CARTO.

## Causas probables que se corrigieron
1. **El puente JavaScript era una clase privada** (`private class MapBridge`). WebView la invoca por reflexión y, hasta donde sé, una clase no pública puede hacer que `onMapReady()` nunca llegue a Kotlin. Entonces `pageReady` quedaba en falso y **los datos nunca se enviaban al mapa**. Ahora es pública (`@Keep`) y además la app ya no depende del puente: considera la página lista cuando el WebView termina de cargarla (`onPageFinished`).
2. **Mosaicos de OpenStreetMap.** `tile.openstreetmap.org` exige la cabecera Referer y un WebView que abre la página desde `file://` no la envía, así que los mosaicos suelen salir en blanco. `map.html` ahora usa el mapa base claro de CARTO (el que ya decía la atribución de la app), que no la exige.
3. **Tamaño del mapa.** Se recalcula el tamaño (`invalidateSize`) al cargar, al recibir datos y 0.4 s después, por si el WebView cambió de tamaño tras crearse.

## Aviso en pantalla
A los 6 segundos de enviar los datos, la app le pregunta al mapa si está bien y, si no, muestra una línea roja abajo, por ejemplo:
- "No se dibujó ninguna zona en el mapa."
- "El mapa base no carga (N mosaicos con error). Revisa la conexión a Internet del teléfono."
- "El mapa tiene tamaño 0."
- "El script del mapa no responde (TerraMap no existe)."
- "La página del mapa no cargó: ..."
Si no aparece ninguna línea, el mapa está sano.

## Si sigue en blanco: dos formas de ver el error real
**A) Logcat** (PowerShell, con el teléfono conectado):
```
adb logcat -s TerraMap
```
Verás "Página cargada: ...", "setData: 5 zonas, mapa 411x790 px", los errores de JavaScript y las URLs de mosaicos que fallan.

**B) Inspeccionar el WebView desde la PC** (ya está habilitado en el código):
1. En Chrome de la PC abre `chrome://inspect/#devices`.
2. Con la app abierta en la pestaña Mapa, busca el WebView de `com.terrascope.app` y pulsa *inspect*.
3. En *Console* se ven los errores de JavaScript; en *Network*, los mosaicos con estado 403/404 o bloqueados.

## Notas
- El teléfono necesita Internet para el mapa base (las zonas y el resto de la app funcionan con la API).
- La API se pide a `http://127.0.0.1:3000/api`. Con Wi-Fi debugging eso solo funciona si hiciste `adb reverse tcp:3000 tcp:3000`; hay que repetirlo cada vez que se reconecta el teléfono.
- Los mosaicos de CARTO son gratuitos con atribución y para uso moderado: suficiente para esta actividad. La atribución "© OpenStreetMap contributors © CARTO" ya está visible en la app.
