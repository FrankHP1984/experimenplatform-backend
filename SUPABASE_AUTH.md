# Autenticación con Supabase

Este proyecto usa **Supabase Auth** para gestionar usuarios y autenticación.

## ¿Qué cambió?

- ❌ **Eliminado**: Endpoints `/api/auth/register` y `/api/auth/login`
- ❌ **Eliminado**: JWT custom, `JwtUtil`, `JwtAuthenticationFilter`
- ✅ **Nuevo**: Autenticación se hace en el frontend con Supabase
- ✅ **Nuevo**: El backend solo valida tokens de Supabase

## Configuración en Supabase

### 1. Habilitar Email Auth

En tu proyecto Supabase:
1. Ve a **Authentication → Providers**
2. Asegúrate de que **Email** esté habilitado
3. (Opcional) Habilita **Google**, **GitHub**, etc.

### 2. Configurar CORS

En **Project Settings → API**:
- Añade `http://localhost:3000` y `http://localhost:5173` a los orígenes permitidos

## Uso en el Frontend

### Instalación

```bash
npm install @supabase/supabase-js
```

### Configuración

```typescript
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://bdmncyrdpmorxhhmepeq.supabase.co'
const supabaseAnonKey = 'tu_anon_key_aqui'

export const supabase = createClient(supabaseUrl, supabaseAnonKey)
```

### Registro

```typescript
const { data, error } = await supabase.auth.signUp({
  email: 'user@example.com',
  password: 'password123',
  options: {
    data: {
      role: 'RESEARCHER' // o 'PARTICIPANT'
    }
  }
})
```

### Login

```typescript
const { data, error } = await supabase.auth.signInWithPassword({
  email: 'user@example.com',
  password: 'password123'
})

// El token está en data.session.access_token
const token = data.session.access_token
```

### Logout

```typescript
await supabase.auth.signOut()
```

### Reset Password

```typescript
await supabase.auth.resetPasswordForEmail('user@example.com', {
  redirectTo: 'http://localhost:3000/reset-password'
})
```

## Llamar a la API del Backend

Una vez autenticado, usa el token en todas las peticiones:

```typescript
const { data: session } = await supabase.auth.getSession()
const token = session?.session?.access_token

const response = await fetch('http://localhost:8080/api/experiments', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
```

## Cómo Funciona

1. **Frontend**: Usuario se registra/loguea con Supabase
2. **Supabase**: Devuelve un JWT con el `user_id` y `email`
3. **Frontend**: Envía el JWT en el header `Authorization: Bearer <token>`
4. **Backend**: `SupabaseAuthenticationFilter` valida el token
5. **Backend**: Extrae el `user_id` del token y lo usa en los controllers

## Obtener el User ID en los Controllers

El filtro guarda el `supabaseId` en los detalles de autenticación:

```java
@GetMapping("/me")
public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
    String supabaseId = (String) authentication.getDetails();
    // Buscar usuario por supabaseId...
}
```

## Sincronizar Usuarios

Cuando un usuario se registra en Supabase, debes crear el registro en tu tabla `users`:

### Opción 1: Desde el Frontend (después del registro)

```typescript
const { data, error } = await supabase.auth.signUp({
  email: 'user@example.com',
  password: 'password123'
})

if (data.user) {
  // Crear usuario en tu backend
  await fetch('http://localhost:8080/api/users/sync', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${data.session.access_token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      supabaseId: data.user.id,
      email: data.user.email,
      role: 'RESEARCHER'
    })
  })
}
```

### Opción 2: Con Supabase Database Webhooks (Avanzado)

Configura un webhook en Supabase que llame a tu backend cuando se cree un usuario.

## Ventajas de Supabase Auth

✅ **Registro y login ya implementados**
✅ **Verificación de email automática**
✅ **Reset password con emails**
✅ **OAuth (Google, GitHub, etc.) gratis**
✅ **Refresh tokens automáticos**
✅ **Menos código que mantener**

## Notas Importantes

- La tabla `users` ya no tiene campo `password` (Supabase lo gestiona)
- Añadimos campo `supabaseId` para vincular con `auth.users`
- El backend solo valida tokens, no gestiona autenticación
- Los tokens de Supabase expiran automáticamente (configurable)
