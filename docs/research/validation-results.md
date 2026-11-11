# Resultados de la validación con datos reales (Etapa 0.5)

Fecha: 2026-11-06 · Ejecutado por el usuario con `scripts/validate-data.mjs`.

## Census ACS 5 años 2024 (llamada real, con clave)
**Somerton city, Arizona (place 04-68080)**

| Indicador | Valor | Margen de error |
|---|---|---|
| Población | 14,574 | ±19 |
| Ingreso mediano del hogar | $74,015 | ±$11,292 |
| Edad mediana | 31.3 años | - |
| Hogares | 4,964 | - |

**Block groups del condado de Yuma:** unos 165 registros (conteo aproximado). Sus datos tienen **márgenes de error muy altos** y no sirven como cifra puntual:
- Ejemplos: ingreso $162,250 ±$134,118; población 748 ±320; población 586 ±628.
- Varios no tienen ingreso mediano (valor nulo).
- Hay valores atípicos (edad mediana 10.7 o 77.7 en distintos grupos).
- Los GEOID no indican a qué ciudad pertenecen: **falta un archivo de geometrías (TIGER) para saber cuáles caen dentro de Somerton**.

**Tracts:** la consulta falló una vez con "Invalid Key" aun con clave válida (otras consultas con la misma clave sí respondieron). Probablemente transitorio; repetir.

## OpenStreetMap (caja aproximada alrededor de Somerton)
- 68 lugares (shop/amenity/office/craft); **40 con nombre**.
- 24 son estacionamientos. Comercio real: 6 comida rápida, 5 tiendas de conveniencia, 4 gasolineras, 3 refacciones, 2 variety store, 1 supermercado, 6 escuelas, 2 clínicas.
- 53 vías principales, con nombres reales (Main Street, Somerton Avenue, Cesar Chavez Avenue, etc.).
- Overpass devolvió un 502 transitorio en una corrida; el servidor de respaldo funcionó.

## Conclusiones
1. **Demografía: viable y real** a nivel ciudad y, con cautela, tract.
2. **Vías y accesibilidad: viable.**
3. **Competidores/negocios: cobertura escasa.** Complementar con captura manual o datos de demostración marcados.
4. **Zonas pequeñas:** usar tracts (o ciudad completa) como unidad principal y mostrar siempre el margen de error.
5. Las cifras de agregadores (14,574 y $74,015) coinciden con el ACS 2024; las diferencias vistas antes eran de otros años o mezclaban ZIP. Aun así, usar solo el Census.
