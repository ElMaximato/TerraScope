# TerraScope — Pruebas de extremo a extremo y datos de demostración (Etapa 10B)

## 1. Datos de demostración

```
cd backend
npm run db:seed-demo
```

Crea (o reinicia) el usuario **demo@terrascope.local** (contraseña `Demo-TerraScope-2026`, o la que pongas en `DEMO_PASSWORD`) con 5 propiedades, 2 zonas guardadas y 4 puntos de campo. Todo lleva el prefijo **[DEMO]** y queda dentro de zonas reales de Somerton (los puntos se calculan dentro de cada polígono). Es seguro repetirlo: solo reinicia los datos del usuario demo.

## 2. Prueba automática del backend (con el servidor en marcha)

```
npm start          # terminal 1
npm run e2e        # terminal 2
```

Simula lo que hacen el reloj y el teléfono y comprueba:

- registro, login (contraseña mala → 401), zonas con polígono;
- **reloj → backend:** sincronizar un punto, reenviarlo (red inestable → no se duplica), cambiar su etiqueta (se actualiza);
- editar el nombre del punto; registrar y editar una propiedad (queda asociada a su zona);
- puntaje, ranking, reporte y análisis de ubicaciones; zonas guardadas;
- **aislamiento entre usuarios:** un segundo usuario no ve, edita ni borra nada del primero.

Los usuarios `e2e.a.<número>@gmail.com` y `e2e.b.<número>@gmail.com` quedan en la base (no hay endpoint para borrar cuentas; no se envía ningún correo); se pueden quitar con `DELETE FROM users WHERE email LIKE 'e2e.%@gmail.com';`.

## 3. Lista de prueba manual (reloj + teléfono)

Marca cada caso al probarlo.

| # | Caso | Resultado esperado |
|---|---|---|
| 1 | Marcar un punto en el reloj con teléfono y backend activos | Vibra, el reloj lo muestra como enviado y aparece en Campo del teléfono |
| 2 | Elegir tipo en el reloj (terreno potencial, etc.) después de marcar | El teléfono muestra el tipo elegido, no "Investigar" |
| 3 | Marcar con el **backend apagado** | El reloj guarda el punto ("por enviar"); al encender el backend, en ≤1 min con la app abierta (o con "Sincronizar") pasa a enviado |
| 4 | Marcar con el **teléfono apagado o lejos** | El punto queda en el reloj; al reconectar se envía solo |
| 5 | Marcar con el teléfono **sin sesión iniciada** | Queda pendiente; al iniciar sesión se sube |
| 6 | Marcar sin permiso de ubicación / en interiores | El reloj lo avisa y no guarda un punto sin coordenadas |
| 7 | Reenviar (Sincronizar varias veces) | No aparecen puntos duplicados |
| 8 | Editar nombre/tipo de un punto en el teléfono, luego marcar otro en el reloj | El editado no vuelve a su valor anterior |
| 9 | Borrar un punto en el teléfono | Desaparece y no regresa al sincronizar |
| 10 | "Evaluar este punto" | Formulario de propiedad con ubicación y nombre cargados |
| 11 | Guardar/quitar una zona en el teléfono | La lista del reloj se actualiza |
| 12 | Cambiar el perfil en Invertir | Los puntajes de las zonas guardadas en el reloj cambian |
| 13 | Registrar 2 propiedades → Comparar | Muestra zona, puntaje, precio y precio por m² de cada una |
| 14 | Editar una propiedad (cambiar punto y precio) | El veredicto se actualiza |
| 15 | Con la app abierta y **sin internet** abrir un reporte | Mensaje de error claro, sin cierre de la app |

## 4. Límites conocidos de la prueba

- El script E2E no incluye la app Android ni el reloj: esos casos son manuales (sección 3).
- La IA no se prueba en el E2E (depende de una clave y de la disponibilidad del proveedor).
