# Empiria — Documento de traspaso para el proyecto React

Este documento resume todo el contexto necesario para construir el frontend React de Empiria a partir de los prototipos HTML ya diseñados. Pégalo al inicio de una sesión nueva y arranca directamente con el setup.

---

## 1. Descripción del proyecto

**Empiria** es una plataforma SaaS para investigadores académicos que permite diseñar, ejecutar y analizar experimentos con participantes humanos. Tiene dos tipos de usuario principales:

- **Investigador (RESEARCHER):** crea y gestiona experimentos, invita participantes, analiza respuestas.
- **Participante (PARTICIPANT):** accede por enlace de invitación, completa cuestionarios por fases.

**Backend:** Java 21 + Spring Boot 4.0.4, REST API en `localhost:8080`. Autenticación via Supabase Auth (JWT como Bearer token en cada petición).

---

## 2. Design system

### Colores (CSS custom properties)
```css
--bg:       #080F1E;   /* Fondo principal */
--surface:  #0D1729;   /* Sidebar, paneles secundarios */
--card:     #111E35;   /* Cards y modales */
--border:   rgba(255,255,255,0.07);
--violet:   #6C4DE6;   /* Acción primaria, investigador */
--violet-d: #5538CC;
--cyan:     #00D4AA;   /* Acción de éxito, participante */
--cyan-d:   #00B891;
--text:     #E8EEF8;
--muted:    #6B7A99;
--danger:   #E05C6B;
--warn:     #F0A500;
--success:  #22C55E;
--blue:     #60A5FA;
```

### Tipografía
- Fuente: **Inter** (Google Fonts)
- Pesos usados: 300, 400, 500, 600, 700

### Principios visuales
- Fondo oscuro navy profundo (`#080F1E`) como base
- Violeta eléctrico (`#6C4DE6`) para acciones del investigador
- Cyan científico (`#00D4AA`) para confirmaciones, participantes y éxito
- **Sin emojis en ningún lugar de la interfaz**
- Partículas animadas en canvas en pantallas públicas (landing, error, cuestionario)
- Bordes con `border-radius` de 8–16px según el componente

### Botones
```
btn-primary  → gradiente violet → violet-d, sombra rgba(108,77,230,.3)
btn-cyan     → gradiente cyan → cyan-d, texto #080F1E (oscuro), font-weight 700
btn-ghost    → transparent, border 1px var(--border), hover ligero
btn-danger   → transparent, border danger, color danger
```

---

## 3. Arquitectura del frontend React

### Stack recomendado
```
Vite + React 18
React Router v6
CSS Modules o styled-components
Axios para llamadas a la API
Supabase JS client para auth
Zustand para estado global ligero (usuario, experimento activo)
```

### Estructura de carpetas sugerida
```
src/
  assets/
  components/
    layout/
      Sidebar.jsx          ← sidebar investigador
      SidebarParticipant.jsx
      Topbar.jsx
      ParticleCanvas.jsx   ← canvas reutilizable
    ui/
      Button.jsx
      Card.jsx
      Badge.jsx
      Modal.jsx
      Toggle.jsx
      ProgressBar.jsx
      ScaleInput.jsx       ← selector escala 1-10
      BooleanInput.jsx     ← Si/No cards
  pages/
    auth/
      Login.jsx
      Register.jsx
      ForgotPassword.jsx
      ResetPassword.jsx
      VerifyEmail.jsx
    public/
      Landing.jsx
      ParticipantInvite.jsx
    researcher/
      Onboarding.jsx
      Dashboard.jsx
      ExperimentDetail.jsx
      ExperimentWizard.jsx
      ParticipantDetail.jsx
      ResponsesAnalytics.jsx
      ProfileResearcher.jsx
    participant/
      DashboardParticipant.jsx
      StudyDetail.jsx
      Questionnaire.jsx
      ProfileParticipant.jsx
    errors/
      NotFound.jsx         ← 404
      TokenExpired.jsx
      AccessDenied.jsx
      ExperimentGone.jsx
      ServerError.jsx
  hooks/
    useAuth.js
    useExperiments.js
    useParticipants.js
    useResponses.js
  api/
    client.js              ← axios instance con interceptor JWT
    experiments.js
    phases.js
    groups.js
    questions.js
    enrollments.js
    responses.js
  store/
    authStore.js           ← zustand: user, role, token
    experimentStore.js
  router/
    index.jsx              ← React Router con rutas protegidas
```

---

## 4. Rutas de la aplicación

```
/                          → Landing (público)
/login                     → Login (público)
/register                  → Registro (público)
/forgot-password           → Olvidé contraseña (público)
/reset-password            → Nueva contraseña vía email (público)
/verify-email              → Verificar email tras registro (público)
/invite/:token             → ParticipantInvite (público)

/onboarding                → Onboarding investigador (auth: RESEARCHER)
/dashboard                 → Dashboard investigador (auth: RESEARCHER)
/experiments/:id           → ExperimentDetail (auth: RESEARCHER)
/experiments/:id/wizard    → ExperimentWizard (auth: RESEARCHER)
/experiments/:id/participants/:participantId → ParticipantDetail
/experiments/:id/analytics → ResponsesAnalytics
/profile                   → ProfileResearcher (auth: RESEARCHER)

/participant/dashboard     → DashboardParticipant (auth: PARTICIPANT)
/participant/study/:id     → StudyDetail (auth: PARTICIPANT)
/participant/study/:id/questionnaire → Questionnaire (auth: PARTICIPANT)
/participant/profile       → ProfileParticipant (auth: PARTICIPANT)

/404                       → NotFound
/error/token-expired       → TokenExpired
/error/access-denied       → AccessDenied
/error/server              → ServerError
```

### Protección de rutas
- `<PrivateRoute role="RESEARCHER">` — redirige a `/` si no autenticado o rol incorrecto
- `<PrivateRoute role="PARTICIPANT">` — ídem para participantes
- Las rutas públicas redirigen al dashboard si ya hay sesión activa

---

## 5. Autenticación (Supabase)

```js
// Flujo investigador
supabase.auth.signUp({ email, password })     // registro
supabase.auth.signInWithPassword(...)          // login
supabase.auth.getSession()                     // recuperar JWT

// Interceptor axios — añadir token a cada petición
axios.interceptors.request.use(config => {
  const token = supabase.auth.getSession()?.access_token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// El backend lee el JWT y extrae el rol (RESEARCHER / PARTICIPANT)
```

---

## 6. Endpoints del backend (28 en total)

### Auth / Usuario
```
POST   /api/auth/register          → registrar investigador
POST   /api/auth/login             → login (devuelve JWT)
GET    /api/users/me               → perfil del usuario autenticado
PUT    /api/users/me               → actualizar perfil
```

### Experimentos
```
GET    /api/experiments            → listar experimentos del investigador
POST   /api/experiments            → crear experimento
GET    /api/experiments/:id        → detalle
PUT    /api/experiments/:id        → editar
DELETE /api/experiments/:id        → eliminar
PATCH  /api/experiments/:id/status → cambiar estado (DRAFT→ACTIVE→PAUSED→FINISHED)
```

### Fases
```
GET    /api/experiments/:id/phases          → listar fases
POST   /api/experiments/:id/phases         → crear fase
PUT    /api/experiments/:id/phases/:phaseId → editar fase
DELETE /api/experiments/:id/phases/:phaseId → eliminar fase
```

### Grupos
```
GET    /api/experiments/:id/groups
POST   /api/experiments/:id/groups
PUT    /api/experiments/:id/groups/:groupId
DELETE /api/experiments/:id/groups/:groupId
```

### Preguntas
```
GET    /api/phases/:phaseId/questions
POST   /api/phases/:phaseId/questions
PUT    /api/phases/:phaseId/questions/:questionId
DELETE /api/phases/:phaseId/questions/:questionId
```

### Participantes / Inscripciones
```
GET    /api/experiments/:id/enrollments           → listar participantes inscritos
POST   /api/invite/:token/enroll                  → inscribir participante via token
GET    /api/invite/:token                         → info pública del experimento por token
PATCH  /api/enrollments/:enrollmentId/status      → cambiar estado (ACTIVE→WITHDRAWN)
GET    /api/enrollments/:enrollmentId             → detalle participante
```

### Respuestas
```
POST   /api/enrollments/:enrollmentId/responses   → enviar respuestas de una fase
GET    /api/experiments/:id/responses             → todas las respuestas (investigador)
GET    /api/enrollments/:enrollmentId/responses   → respuestas de un participante
```

---

## 7. Entidades principales

```
Experiment {
  id, title, description,
  design: PRETEST_POSTTEST | BETWEEN_SUBJECTS | LONGITUDINAL,
  status: DRAFT | ACTIVE | PAUSED | FINISHED | CANCELLED,
  startDate, endDate, createdAt
}

Phase { id, experimentId, name, description, order, startDate, endDate }

Group { id, experimentId, name, description, color }

Question {
  id, phaseId, text, type: TEXT | NUMBER | SCALE | MULTIPLE_CHOICE | BOOLEAN,
  required, order, options (array, solo para MULTIPLE_CHOICE),
  unit, min, max (para NUMBER), labelMin, labelMax (para SCALE)
}

Participant { id, name, surname, email, dateOfBirth, sex }

Enrollment {
  id, experimentId, participantId, groupId,
  status: PENDING | ACTIVE | COMPLETED | WITHDRAWN,
  enrolledAt, customFields (JSON)
}

Response { id, enrollmentId, phaseId, questionId, value, answeredAt }
```

---

## 8. Pantallas prototipadas (ficheros HTML de referencia)

Todos en `C:\proyectos\experimentplatform\`:

| Fichero | Pantalla | Usuario |
|---|---|---|
| `landing-prototype.html` | Landing pública | Público |
| `onboarding-researcher.html` | Onboarding post-registro | Investigador |
| `dashboard-researcher.html` | Panel principal | Investigador |
| `experiment-detail.html` | Detalle experimento (5 tabs) | Investigador |
| `experiment-wizard.html` | Wizard creación (5 pasos) | Investigador |
| `participant-invite.html` | Landing invitación | Participante |
| `participant-dashboard.html` | Panel principal | Participante |
| `participant-detail.html` | Detalle participante | Investigador |
| `questionnaire.html` | Cuestionario standalone | Participante |
| `profile-researcher.html` | Perfil investigador | Investigador |
| `profile-participant.html` | Perfil participante | Participante |
| `responses-analytics.html` | Análisis de respuestas | Investigador |
| `auth-pages.html` | Login, registro, olvidé contraseña, nueva contraseña, verificar email | Público |
| `participant-study-detail.html` | Detalle de estudio inscrito | Participante |
| `error-pages.html` | 404, token, 403, gone, 500 | Todos |

---

## 9. Decisiones de diseño tomadas

- **Sin sidebar** en el cuestionario ni en el wizard — experiencia enfocada
- **Particle canvas** solo en: landing, error-pages, questionnaire
- El color **cyan** identifica al participante; el **violeta** al investigador
- Los botones destructivos siempre requieren confirmación modal
- En el wizard el panel derecho actualiza en tiempo real el resumen del experimento
- Los participantes acceden **solo por enlace** — no hay autoregistro público
- La creación de cuenta del participante es **opcional** (pueden responder sin cuenta, pero no volver a acceder)
- Los tipos de pregunta son: `TEXT`, `NUMBER`, `SCALE`, `MULTIPLE_CHOICE`, `BOOLEAN`
- Las escalas son siempre 1–10
- El estado del experimento sigue el flujo: `DRAFT → ACTIVE → PAUSED → FINISHED / CANCELLED`

---

## 10. Prompt de inicio para sesión nueva

Pega esto al inicio de una sesión nueva en Cowork:

> Vamos a construir el frontend React de **Empiria**, una plataforma SaaS para experimentos académicos. El backend en Spring Boot ya está construido y expone una REST API en localhost:8080 con autenticación Supabase JWT. Tenemos 15 pantallas prototipadas en HTML que sirven como referencia visual exacta. El fichero `REACT_HANDOFF.md` en `C:\proyectos\experimentplatform\experimenplatform-backend\` contiene todo el contexto: design system, rutas, endpoints, entidades y estructura de carpetas. Lee ese fichero primero y luego arrancamos con el setup del proyecto React con Vite.
