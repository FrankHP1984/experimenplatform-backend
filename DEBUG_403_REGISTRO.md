# Diagnóstico: Error 403 en Registro de Usuario

## Problema
Al registrar un nuevo usuario, se recibe un error 403 (Acceso Denegado) cuando el frontend intenta sincronizar el usuario con el backend mediante `/api/users/sync`.

## Flujo Esperado
1. Usuario se registra en Supabase (frontend)
2. Supabase genera un JWT válido
3. Frontend recibe el token y lo almacena
4. Frontend llama a `POST /api/users/sync` con el token en el header `Authorization: Bearer <token>`
5. Backend valida el token, crea el usuario en la DB local y devuelve 201 Created
6. Usuario puede usar la aplicación

## Flujo Actual (con error)
1. Usuario se registra en Supabase ✅
2. Supabase genera un JWT válido ✅
3. Frontend recibe el token ✅
4. Frontend llama a `POST /api/users/sync` ❌ **Error 403**
5. Supabase envía email de confirmación (esto indica que el registro en Supabase fue exitoso)

## Posibles Causas

### A) Problema en el Backend
- El token no se está validando correctamente
- El endpoint `/api/users/sync` no está configurado como accesible para usuarios con `ROLE_AUTHENTICATED`
- El filtro `SupabaseAuthenticationFilter` está rechazando el token
- La URL de JWKS de Supabase es incorrecta

### B) Problema en el Frontend
- El token no se está enviando en el header `Authorization`
- El token se está enviando en un formato incorrecto
- La petición se está haciendo antes de que el token esté disponible

## Pasos de Diagnóstico

### 1. Verificar logs del backend
Buscar en la consola de IntelliJ cuando se intenta registrar:
```
- "Token procesado correctamente. SupabaseId: ..., Email: ..."
- "Usuario NO encontrado en DB, asignando ROLE_AUTHENTICATED"
- "Autenticación establecida correctamente para: ..."
- O cualquier error: "Error procesando token de Supabase: ..."
```

**Si NO aparecen estos logs:**
- El token no está llegando al backend
- El token no tiene el formato correcto
- **Problema en el FRONTEND**

**Si aparecen logs de error:**
- El token es inválido
- La configuración de JWKS es incorrecta
- **Problema en el BACKEND**

**Si aparecen logs exitosos pero aún hay 403:**
- El usuario se autenticó pero Spring Security rechaza el acceso
- **Problema en la configuración de seguridad del BACKEND**

### 2. Verificar petición en el navegador
Abrir DevTools (F12) → Network → Intentar registrarse → Buscar la petición a `/api/users/sync`

**Verificar:**
- ¿La petición tiene el header `Authorization: Bearer <token>`?
- ¿Cuál es el código de respuesta exacto? (403, 401, 500, etc.)
- ¿Hay algún mensaje de error en la respuesta?

### 3. Verificar configuración de Supabase
En `.env` del backend:
```
SUPABASE_URL=https://bdmncyrdpmorxhhmepeq.supabase.co
```

En el frontend (`.env` o similar):
```
VITE_SUPABASE_URL=https://bdmncyrdpmorxhhmepeq.supabase.co
```

**Deben ser EXACTAMENTE iguales** (incluyendo https://, sin trailing slash)

### 4. Verificar endpoint de JWKS
Probar manualmente en el navegador:
```
https://bdmncyrdpmorxhhmepeq.supabase.co/auth/v1/jwks
```

**Debe devolver un JSON con las claves públicas:**
```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "...",
      "n": "...",
      "e": "AQAB",
      ...
    }
  ]
}
```

**Si devuelve 404 o error:**
- La URL de JWKS es incorrecta
- Probar con: `/auth/v1/.well-known/jwks.json`

## Archivos Modificados en esta Sesión

### Backend
1. `SecurityConfig.java` - Configuración de endpoints públicos y autenticados
2. `SupabaseAuthenticationFilter.java` - Filtro que valida tokens JWT de Supabase
3. `SupabaseJwtConfig.java` - Configuración de JWKS y validación de tokens
4. `UserController.java` - Endpoint `/api/users/sync`
5. `OwnershipChecker.java` - Agregado método `checkExperimentOwnership`

### Configuración
1. `.env` - Variables de entorno (SUPABASE_URL, encryption.key, etc.)
2. `pom.xml` - Dependencia `dotenv-java`
3. `ExperimentplatformApplication.java` - Carga de variables de entorno desde `.env`

## Estado Actual de la Configuración

### SecurityConfig.java
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/invitations/*").permitAll()
    .requestMatchers("/api/users/sync").authenticated()  // Requiere token válido
    .anyRequest().authenticated()
)
```

### SupabaseAuthenticationFilter.java
- Usuarios nuevos (no en DB) reciben `ROLE_AUTHENTICATED`
- Usuarios existentes reciben su rol real (RESEARCHER, PARTICIPANT, etc.)
- Logging detallado para diagnóstico

## DIAGNÓSTICO COMPLETADO ✅

### Hallazgos:
1. ✅ La petición que falla es `GET /api/users/me` (NO `/api/users/sync`)
2. ❌ **La petición NO tiene el header `Authorization: Bearer <token>`**
3. ❌ El frontend NO está llamando a `/api/users/sync` después del registro
4. ❌ El frontend está intentando obtener el perfil del usuario sin autenticación

### Conclusión:
**EL PROBLEMA ESTÁ EN EL FRONTEND**

El frontend no está:
- Enviando el token de Supabase en las peticiones
- Llamando a `/api/users/sync` para sincronizar el usuario después del registro
- Manejando correctamente el flujo de autenticación

### Solución:
El frontend debe:
1. Después del registro en Supabase, obtener el token JWT
2. Llamar a `POST /api/users/sync` con el header `Authorization: Bearer <token>`
3. Solo después de sincronizar, llamar a `GET /api/users/me`
4. Todas las peticiones autenticadas deben incluir el header `Authorization`

### Cambios realizados en el backend:
- Agregado logging en `/api/users/me` para diagnosticar
- El backend está configurado correctamente
- El problema NO está en el backend

## Soluciones Temporales (NO RECOMENDADAS PARA PRODUCCIÓN)

### Si necesitas probar urgentemente sin autenticación:
```java
// En SecurityConfig.java
.anyRequest().permitAll()  // TEMPORAL: Permitir todo
```

### Si el problema es la validación del token:
```java
// En SupabaseJwtConfig.java
// Cambiar la URL de JWKS o el algoritmo
```

## Notas Importantes
- El email de confirmación de Supabase indica que el registro en Supabase fue exitoso
- El problema está en la sincronización con el backend local
- El error 403 significa "Forbidden" (autenticado pero sin permisos)
- El error 401 significa "Unauthorized" (no autenticado)
- Si es 403, el token se está validando pero Spring Security rechaza el acceso
- Si es 401, el token no se está validando correctamente
