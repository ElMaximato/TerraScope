# Emparejar el emulador del teléfono con el del reloj

Fuente: https://developer.android.com/training/wearables/get-started/connect-phone (consultada 2026-11-06).

## Requisitos
- Emulador de **teléfono** con **Android 11 (API 30) o superior** y **Google Play Store** (en Device Manager la imagen muestra el icono de Play).
- Emulador de **reloj** (Wear OS) creado en Device Manager.
- Imágenes de sistema actualizadas desde el SDK Manager si hace falta.

## Pasos
1. Abre Device Manager y arranca el emulador del teléfono.
2. En el teléfono, abre **Play Store**, inicia sesión con una cuenta de Google e instala/actualiza la app acompañante de relojes (**Google Pixel Watch**, `com.google.android.apps.wear.companion`). Ábrela una vez.
3. En Device Manager, menú ⋮ del reloj (o del teléfono) > **Pair Wearable**.
4. En el asistente elige el reloj y el teléfono y pulsa **Next**.
5. Espera varios minutos mientras Android Studio prepara ambos emuladores. No lo interrumpas.
6. Comprueba: en Device Manager, ⋮ > **View Details** (ventana *Paired Devices*) muestra el par; los dispositivos emparejados llevan un icono pequeño.

## Si falla
- Revisa que el teléfono sea API 30+ y tenga Play Store.
- Actualiza las imágenes de sistema en el SDK Manager.
- Espera: el primer emparejado tarda.
- La documentación no describe un reinicio manual de emparejados de emuladores.

## Notas del proyecto
- La app del reloj y la del teléfono comparten `applicationId = com.terrascope.app` (requisito de la Data Layer).
- La sincronización real (Data Layer) se implementa en la Etapa 8; este paso solo deja los emuladores emparejados.
