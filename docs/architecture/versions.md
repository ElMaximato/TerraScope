# Versiones del entorno y del proyecto

Fuente: archivos generados por Android Studio y comandos del usuario (2026-11-06). Nada de esto es supuesto.

## Entorno del autor
| Herramienta | Versión |
|---|---|
| Node | 26.7.0 |
| npm | 11.19.0 |
| Java (sistema) | 17.0.12 |
| Git | 2.55.0 |
| MySQL Server | 8.0.46 (confirmado por `/api/health`) |
| Android Studio | por confirmar |
| Emuladores | Medium Phone API 37.0 (Android 17) y Wear OS Large Round (Android 17), emparejados |
| Gradle (wrapper) | 9.6.0 (confirmado en la compilación) |

## Android (`android/gradle/libs.versions.toml`)
| Componente | Versión |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.2.10 |
| Compose BOM (teléfono y base del reloj) | 2026.02.01 |
| compileSdk / targetSdk | 37 |
| minSdk teléfono | 26 |
| minSdk reloj | 30 |
| Wear Compose Material3 / Foundation | 1.5.6 |
| play-services-wearable | 18.0.0 |

## Decisión: applicationId compartido
Teléfono y reloj usan `applicationId = com.terrascope.app`. La Wearable Data Layer lo requiere para emparejar ambas apps. Los `namespace` siguen distintos (`com.terrascope.app` y `com.terrascope.wear`).

## Backend (`backend/package.json`)
express ^4.21.2, mysql2 ^3.11.0, dotenv ^16.4.5, cors ^2.8.5.
