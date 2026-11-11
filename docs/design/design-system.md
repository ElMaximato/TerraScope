# Sistema de diseño (Etapa 1)

**Dirección:** minimalista, claro, premium; poca información pero importante. Referencia: app inmobiliaria clara con verde oscuro y dorado. Sin emojis como iconos.

> La especificación original proponía modo oscuro como principal. Por tu indicación, **el tema claro es el principal en el teléfono**. El reloj usa fondo oscuro (convención de Wear OS y ahorro de batería en OLED). Modo oscuro del teléfono: opcional, después.

## Color
| Token | Valor | Uso |
|---|---|---|
| `background` | #F6F8F7 | Fondo de pantallas |
| `surface` | #FFFFFF | Tarjetas, hojas |
| `primary` | #0F4D3A | Botones principales, selección, íconos activos |
| `primary-soft` | #E3F0EA | Fondos de chips y estados activos |
| `accent` | #E3AE55 | Acento dorado: acción destacada, "pendiente", score alto |
| `text` | #14211C | Texto principal |
| `text-muted` | #6B7A74 | Texto secundario, unidades |
| `line` | #E4EAE7 | Divisores y bordes |
| `success` / `warning` / `danger` | #2E8B57 / #C98A1B / #C4473A | Estados |

**Etiquetas de tipo de dato (chips pequeños)**
- Oficial: verde suave · Estimación: gris azulado · Derivado: gris · Aportado por ti: borde dorado.

**Reloj:** fondo #0B1512, texto #EAF2EE, primario #3DBE8E, acento #E3AE55.

## Tipografía
Inter (la misma del diseño base de Figma). Si da problemas en Android Studio, usar la fuente del sistema.
| Rol | Tamaño / peso |
|---|---|
| Título de pantalla | 24 / SemiBold |
| Título de tarjeta | 17 / SemiBold |
| Cuerpo | 15 / Regular |
| Dato grande (score, cifra) | 32 / Bold |
| Etiqueta / nota | 12 / Medium |
Reloj: cuerpo 14, dato grande 28.

## Forma y espacio
- Rejilla de 8 dp. Márgenes de pantalla 20 dp.
- Esquinas: tarjetas 20 dp, botones 16 dp, chips 12 dp.
- Sombras casi imperceptibles (elevación 1-2) y bordes finos `line`.
- Objetivo táctil mínimo 48 dp (teléfono) y 48 dp (reloj).

## Componentes base
1. **Barra superior:** título, acción a la derecha.
2. **Barra inferior:** 4 pestañas, ícono + etiqueta, la activa en `primary`.
3. **Tarjeta de zona:** nombre, score en anillo, confianza, botón guardar.
4. **Fila de indicador:** nombre, valor, unidad, chip de tipo, fuente y año en texto pequeño.
5. **Anillo de score:** número al centro; debajo "Confianza: Alta/Media/Baja" (separada del score).
6. **Hoja inferior** del mapa con zona seleccionada.
7. **Chips** de filtro y de etiqueta de campo.
8. **Botón principal** (relleno `primary`) y secundario (borde).
9. **Estados:** esqueleto, vacío, error, banda sin conexión, punto dorado de pendiente.

## Íconos
Material Symbols (redondeados, trazo fino). Nada de emojis.

## Accesibilidad
Contraste mínimo 4.5:1 en texto; no depender solo del color (el score lleva número y texto); descripciones para lectores de pantalla en íconos.
