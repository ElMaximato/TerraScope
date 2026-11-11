# ADR-002: Autenticación

**Fecha:** 2026-11-06
**Estado:** Propuesta del asistente, implementada (el usuario delegó la decisión técnica)

## Contexto
La app necesita registro e inicio de sesión (RF-001) y proteger los datos de cada usuario (RF-002, RNF-003). Proyecto escolar: simple, sin servicios externos de pago.

## Opciones consideradas
| Opción | A favor | En contra |
|---|---|---|
| Proveedor externo (p. ej. Firebase Auth) | Recuperación de contraseña lista | Cuenta y configuración externas; más piezas que el usuario debe mantener |
| **Correo + contraseña propios, hash scrypt y token JWT** | Sin servicios externos; todo en el backend; fácil de probar | Sin recuperación de contraseña; hay que cuidar el secreto del token |
| Sesiones de servidor con cookies | Cierre de sesión inmediato | Poco natural para apps móviles |

## Decisión
Autenticación propia en el backend:
- Contraseñas con **scrypt** de `node:crypto` (sin dependencias nativas), sal aleatoria por usuario y comparación en tiempo constante.
- **JWT HS256** (librería `jsonwebtoken`) con caducidad de 7 días, firmado con `JWT_SECRET` (mínimo 32 caracteres, en `.env`, nunca en Git).
- Login con mensaje idéntico para correo inexistente y contraseña incorrecta, y límite de intentos por IP.

## Consecuencias
- No hay recuperación de contraseña ni tokens de refresco: al expirar, el usuario vuelve a iniciar sesión.
- Cerrar sesión consiste en descartar el token en el cliente; el token sigue siendo válido hasta que caduque.
- El rate limit es en memoria y se reinicia con el servidor (suficiente para el prototipo).
- Para desplegar fuera de local haría falta HTTPS (RNF-001).
