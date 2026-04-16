# Ejemplo de Frontend con Supabase Auth

## Configuración Inicial

### 1. Instalar Supabase Client

```bash
npm install @supabase/supabase-js
```

### 2. Crear archivo de configuración

**`src/lib/supabase.ts`** (o `.js` si no usas TypeScript):

```typescript
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://bdmncyrdpmorxhhmepeq.supabase.co'
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkbW5jeXJkcG1vcnhoaG1lcGVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU3MzA2MjEsImV4cCI6MjA5MTMwNjYyMX0.JrQsq66hdk2TNmLDhBSiPq0avBC8HRHDHogss-ye4I4'

export const supabase = createClient(supabaseUrl, supabaseAnonKey)
```

---

## Ejemplos de Uso

### Registro de Usuario

```typescript
import { supabase } from './lib/supabase'

async function register(email: string, password: string, role: 'RESEARCHER' | 'PARTICIPANT') {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: {
      data: {
        role: role // Esto se guarda en user_metadata
      }
    }
  })

  if (error) {
    console.error('Error en registro:', error.message)
    return null
  }

  console.log('Usuario registrado:', data.user)
  
  // IMPORTANTE: Después del registro, crear el usuario en tu backend
  if (data.session) {
    await syncUserWithBackend(data.session.access_token, data.user.id, email, role)
  }

  return data
}

// Sincronizar usuario con tu backend
async function syncUserWithBackend(token: string, supabaseId: string, email: string, role: string) {
  const response = await fetch('http://localhost:8080/api/users/sync', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      supabaseId,
      email,
      role
    })
  })

  if (!response.ok) {
    console.error('Error sincronizando usuario con backend')
  }
}
```

### Login

```typescript
async function login(email: string, password: string) {
  const { data, error } = await supabase.auth.signInWithPassword({
    email,
    password
  })

  if (error) {
    console.error('Error en login:', error.message)
    return null
  }

  console.log('Login exitoso:', data.user)
  console.log('Token:', data.session.access_token)
  
  return data
}
```

### Logout

```typescript
async function logout() {
  const { error } = await supabase.auth.signOut()
  
  if (error) {
    console.error('Error en logout:', error.message)
  } else {
    console.log('Logout exitoso')
  }
}
```

### Obtener Usuario Actual

```typescript
async function getCurrentUser() {
  const { data: { user } } = await supabase.auth.getUser()
  return user
}

async function getCurrentSession() {
  const { data: { session } } = await supabase.auth.getSession()
  return session
}
```

### Reset Password

```typescript
async function resetPassword(email: string) {
  const { error } = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: 'http://localhost:3000/reset-password'
  })

  if (error) {
    console.error('Error enviando email de reset:', error.message)
  } else {
    console.log('Email de reset enviado')
  }
}
```

---

## Llamar a la API del Backend

### Ejemplo: Obtener Experimentos

```typescript
async function getExperiments() {
  // Obtener el token de la sesión actual
  const { data: { session } } = await supabase.auth.getSession()
  
  if (!session) {
    console.error('No hay sesión activa')
    return null
  }

  const response = await fetch('http://localhost:8080/api/experiments', {
    headers: {
      'Authorization': `Bearer ${session.access_token}`,
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    console.error('Error obteniendo experimentos')
    return null
  }

  const experiments = await response.json()
  return experiments
}
```

### Ejemplo: Crear Experimento

```typescript
async function createExperiment(experimentData: any) {
  const { data: { session } } = await supabase.auth.getSession()
  
  if (!session) {
    throw new Error('No hay sesión activa')
  }

  const response = await fetch('http://localhost:8080/api/experiments', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${session.access_token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(experimentData)
  })

  if (!response.ok) {
    throw new Error('Error creando experimento')
  }

  return await response.json()
}
```

---

## Componente React Completo (Ejemplo)

```tsx
import { useState, useEffect } from 'react'
import { supabase } from './lib/supabase'

function App() {
  const [user, setUser] = useState(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [experiments, setExperiments] = useState([])

  // Verificar sesión al cargar
  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setUser(session?.user ?? null)
    })

    // Escuchar cambios en la autenticación
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setUser(session?.user ?? null)
    })

    return () => subscription.unsubscribe()
  }, [])

  // Login
  const handleLogin = async (e) => {
    e.preventDefault()
    const { error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) alert(error.message)
  }

  // Logout
  const handleLogout = async () => {
    await supabase.auth.signOut()
  }

  // Obtener experimentos
  const fetchExperiments = async () => {
    const { data: { session } } = await supabase.auth.getSession()
    
    const response = await fetch('http://localhost:8080/api/experiments', {
      headers: {
        'Authorization': `Bearer ${session.access_token}`
      }
    })
    
    const data = await response.json()
    setExperiments(data.content || [])
  }

  if (!user) {
    return (
      <div>
        <h1>Login</h1>
        <form onSubmit={handleLogin}>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button type="submit">Login</button>
        </form>
      </div>
    )
  }

  return (
    <div>
      <h1>Bienvenido, {user.email}</h1>
      <button onClick={handleLogout}>Logout</button>
      <button onClick={fetchExperiments}>Cargar Experimentos</button>
      
      <ul>
        {experiments.map(exp => (
          <li key={exp.id}>{exp.title}</li>
        ))}
      </ul>
    </div>
  )
}

export default App
```

---

## Variables de Entorno (Recomendado)

En lugar de hardcodear las credenciales, usa variables de entorno:

**`.env`**:
```
VITE_SUPABASE_URL=https://bdmncyrdpmorxhhmepeq.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkbW5jeXJkcG1vcnhoaG1lcGVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU3MzA2MjEsImV4cCI6MjA5MTMwNjYyMX0.JrQsq66hdk2TNmLDhBSiPq0avBC8HRHDHogss-ye4I4
```

**`src/lib/supabase.ts`**:
```typescript
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

export const supabase = createClient(supabaseUrl, supabaseAnonKey)
```

---

## Endpoint de Sincronización en el Backend

Necesitas crear este endpoint en tu backend para sincronizar usuarios:

**`UserController.java`**:
```java
@PostMapping("/users/sync")
public ResponseEntity<UserDTO> syncUser(@RequestBody SyncUserRequest request, Authentication authentication) {
    String supabaseId = (String) authentication.getDetails();
    
    // Verificar que el supabaseId del token coincide con el del request
    if (!supabaseId.equals(request.getSupabaseId())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // Crear o actualizar usuario
    User user = new User(request.getSupabaseId(), request.getEmail(), request.getRole());
    User saved = userRepository.save(user);
    
    return ResponseEntity.ok(userService.convertToDTO(saved));
}
```

---

## Notas Importantes

- ✅ El **anon key es público** - puede estar en el frontend
- ✅ Supabase maneja la seguridad con **Row Level Security (RLS)**
- ✅ Los tokens expiran automáticamente (Supabase los renueva)
- ✅ No necesitas gestionar refresh tokens manualmente
