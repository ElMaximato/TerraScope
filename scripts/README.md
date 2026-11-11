# scripts/

## validate-data.mjs (Etapa 0.5)
Comprueba con llamadas reales que los datos de Somerton existen y cuántos hay.

```bash
cd C:\Users\ferba\Documents\ProyectosUT\inversiones_somerton
node scripts/validate-data.mjs
```
- Requiere Node 18 o superior. No instala dependencias.
- Clave del Census (opcional, gratis): `set CENSUS_API_KEY=tu_clave` (CMD) o `$env:CENSUS_API_KEY="tu_clave"` (PowerShell). Si sin clave falla, regístrala en https://api.census.gov/data/key_signup.html
- Genera `data/samples/validation-report.json` y archivos de muestra.
- La caja geográfica (`BBOX`) es aproximada; ajústala en el script si los negocios salen en cero.
- Datos de OpenStreetMap: © OpenStreetMap contributors (ODbL).
