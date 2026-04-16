# Documento de Especificación del Proyecto

## Plataforma de Diseño y Gestión de Experimentos con Participantes

---

# 1. Descripción general del proyecto

El proyecto consiste en el desarrollo de una **aplicación web para diseñar, configurar y gestionar experimentos con participantes humanos**, permitiendo definir:

* Diseños experimentales estructurados
* Grupos de participantes
* Fases del experimento
* Cuestionarios o registros de datos
* Recogida de respuestas por parte de los participantes

El sistema permitirá que **investigadores diseñen estudios y recojan datos estructurados** de participantes a lo largo del tiempo.

La plataforma no realizará análisis clínicos ni estadísticos complejos, centrándose en la **gestión metodológica y recogida de datos**.

---

# 2. Objetivos funcionales

El sistema debe permitir:

1. Crear y gestionar **experimentos**.
2. Definir **diseños experimentales** (pretest–postest, entre sujetos, longitudinal).
3. Crear **grupos de participantes** dentro de un experimento.
4. Definir **fases temporales del experimento**.
5. Definir **preguntas o formularios de recogida de datos**.
6. Permitir que los **participantes respondan cuestionarios**.
7. Permitir a los investigadores **consultar los datos recogidos**.
8. Gestionar **equipos de investigación** con roles diferenciados.
9. Enviar y gestionar **invitaciones** a participantes por email.
10. Registrar el **consentimiento informado** de los participantes.

---

# 3. Tipos de usuario

## Investigador (RESEARCHER)

Puede:

* Crear y gestionar experimentos
* Definir fases y grupos
* Crear cuestionarios
* Invitar participantes
* Consultar respuestas
* Gestionar equipos de investigación

## Participante (PARTICIPANT)

Puede:

* Registrarse en la plataforma
* Inscribirse en experimentos mediante invitación
* Completar cuestionarios
* Consultar sus propias inscripciones

## Administrador (ADMIN)

Puede:

* Gestionar usuarios
* Supervisar el sistema
* Realizar cualquier acción disponible para RESEARCHER y PARTICIPANT

---

# 4. Arquitectura tecnológica

## Backend

Tecnología principal:

* Java
* Spring Boot
* Spring Data JPA
* Spring Security
* **Supabase Auth** para autenticación (migrado desde JWT custom)
* PostgreSQL como base de datos (gestionado a través de Supabase)

Dependencias externas:

* `com.nimbusds:nimbus-jose-jwt` — parseo de tokens JWT de Supabase

Arquitectura:

* REST API
* Arquitectura MVC

Capas del backend:

```
Controller → Service → Repository → Database
```

---

## Frontend

Tecnología prevista:

* React
* TypeScript
* Supabase JS Client (`@supabase/supabase-js`)
* Consumo de API REST

El frontend incluirá:

* Panel de investigador
* Panel de participante
* Formularios dinámicos

---

# 5. Autenticación — Supabase Auth

El sistema delega completamente la autenticación en **Supabase Auth**. El backend no gestiona contraseñas ni sesiones.

## Flujo de autenticación

```
1. Frontend → Supabase Auth (registro / login)
2. Supabase  → Devuelve JWT con sub (user_id) y email
3. Frontend  → Envía JWT en header: Authorization: Bearer <token>
4. Backend   → SupabaseAuthenticationFilter valida el token
5. Backend   → Extrae supabaseId y lo usa en los controllers
```

## SupabaseAuthenticationFilter

El filtro `security/SupabaseAuthenticationFilter` intercepta todas las peticiones, parsea el JWT de Supabase y establece el contexto de autenticación de Spring Security con:

* **Principal**: email del usuario
* **Details**: `supabaseId` (campo `sub` del JWT)
* **Authorities**: rol extraído del JWT (prefijado con `ROLE_`)

## Sincronización de usuarios

Cuando un participante se registra en Supabase, el frontend debe crear el registro en la tabla local `users` del backend:

```
POST /api/users/sync
Authorization: Bearer <supabase_token>
{ "supabaseId": "...", "email": "...", "role": "RESEARCHER" }
```

## Endpoints de autenticación eliminados

Los siguientes endpoints han sido eliminados al migrar a Supabase Auth:

* ~~`POST /api/auth/register`~~
* ~~`POST /api/auth/login`~~

---

# 6. Modelo de datos

Las entidades principales del sistema son:

---

## User

Representa a cualquier usuario del sistema. Las credenciales son gestionadas por Supabase Auth.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria interna |
| supabaseId | String | ID del usuario en Supabase Auth (`auth.users.id`) |
| email | String | Email único |
| role | UserRole | Rol en el sistema |
| createdAt | LocalDateTime | Fecha de registro |

> **Nota**: el campo `password` ha sido eliminado. Supabase gestiona las credenciales.

---

## Participant

Perfil de participante vinculado a un User. Un usuario con rol PARTICIPANT tiene asociado un perfil Participant.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| user | User | Relación 1:1 con User |
| bio | String | Descripción opcional del participante |
| createdAt | LocalDateTime | Fecha de creación del perfil |

---

## Experiment

Representa un estudio o experimento.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| title | String | Título del experimento |
| description | String | Descripción |
| consentText | String | Texto del consentimiento informado (hasta 5000 caracteres) |
| designType | DesignType | Tipo de diseño experimental |
| startDate | LocalDateTime | Fecha de inicio |
| endDate | LocalDateTime | Fecha de fin |
| status | ExperimentStatus | Estado del experimento |
| allowLateEnrollment | boolean | Permite inscripción tardía |
| owner | User | Investigador propietario |
| team | ResearchTeam | Equipo asignado (opcional) |
| createdAt | LocalDateTime | Fecha de creación |
| updatedAt | LocalDateTime | Última actualización |

Relaciones:

* Un experimento tiene múltiples fases (`Phase`)
* Un experimento tiene múltiples grupos (`Group`)
* Un experimento tiene múltiples inscripciones (`Enrollment`)

---

## Group

Representa un grupo dentro de un experimento (ej: control, experimental).

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| name | String | Nombre del grupo |
| description | String | Descripción opcional |
| experiment | Experiment | Experimento al que pertenece |

---

## Phase

Representa una fase temporal del experimento (ej: Pretest, Intervención, Postest).

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| name | String | Nombre de la fase |
| phaseOrder | int | Orden de la fase |
| startDate | LocalDateTime | Inicio de la fase |
| endDate | LocalDateTime | Fin de la fase |
| experiment | Experiment | Experimento al que pertenece |

---

## Question

Representa una pregunta o elemento de recogida de datos dentro de una fase.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| text | String | Enunciado de la pregunta |
| type | QuestionType | Tipo de respuesta esperada |
| required | boolean | Si es obligatoria |
| orderIndex | int | Posición en el formulario |
| phase | Phase | Fase a la que pertenece |

---

## Enrollment

Representa la inscripción de un participante en un experimento. Reemplaza a la relación directa Participant–Experiment del diseño inicial.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| participant | Participant | Participante inscrito |
| experiment | Experiment | Experimento |
| group | Group | Grupo asignado (opcional) |
| status | EnrollmentStatus | Estado de la inscripción |
| enrolledAt | LocalDateTime | Fecha de inscripción |
| completedAt | LocalDateTime | Fecha de finalización |
| consentSignedAt | LocalDateTime | Fecha de firma del consentimiento |

Restricción: un participante solo puede inscribirse una vez en el mismo experimento (`UNIQUE participant_id, experiment_id`).

---

## Response

Representa una respuesta de un participante a una pregunta dentro de su inscripción.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| enrollment | Enrollment | Inscripción a la que pertenece |
| question | Question | Pregunta respondida |
| textValue | String | Valor de texto (hasta 2000 caracteres) |
| numericValue | Integer | Valor numérico |
| booleanValue | Boolean | Valor booleano |
| submittedAt | LocalDateTime | Fecha de envío |

Restricción: una inscripción solo puede tener una respuesta por pregunta (`UNIQUE enrollment_id, question_id`).

---

## ResearchTeam

Representa un equipo de investigación que puede gestionar múltiples experimentos.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| name | String | Nombre del equipo |
| description | String | Descripción |
| createdAt | LocalDateTime | Fecha de creación |

---

## TeamMembership

Relación entre un User y un ResearchTeam con un rol específico dentro del equipo.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| team | ResearchTeam | Equipo |
| user | User | Miembro |
| role | TeamRole | Rol en el equipo |

---

## Invitation

Representa una invitación enviada a un email para participar en un experimento.

Campos:

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria |
| experiment | Experiment | Experimento al que se invita |
| invitedEmail | String | Email del invitado |
| group | Group | Grupo destino (opcional) |
| token | String | Token UUID único para aceptar la invitación |
| status | InvitationStatus | Estado de la invitación |
| createdAt | LocalDateTime | Fecha de creación |
| expiresAt | LocalDateTime | Fecha de expiración |

---

# 7. Enumeraciones

## UserRole

```
RESEARCHER | PARTICIPANT | ADMIN
```

## ExperimentStatus

```
DRAFT | ACTIVE | FINISHED
```

## DesignType

Tipos de diseño experimental soportados.

## QuestionType

```
TEXT | NUMBER | SCALE | MULTIPLE_CHOICE | BOOLEAN
```

## EnrollmentStatus

```
PENDING | ACTIVE | COMPLETED | WITHDRAWN
```

## InvitationStatus

```
PENDING | ACCEPTED | REJECTED
```

## TeamRole

Roles dentro de un equipo de investigación.

---

# 8. Relaciones del modelo de datos

```
User
├── Experiment (owner)
├── Participant (perfil 1:1)
└── TeamMembership

ResearchTeam
├── TeamMembership
└── Experiment (team)

Experiment
├── Group
├── Phase
│   └── Question
│       └── Response
├── Enrollment
│   └── Response
└── Invitation

Participant
└── Enrollment

Enrollment
└── Response
```

---

# 9. Endpoints de la API

Todos los endpoints están bajo el prefijo `/api`.

## Usuarios

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/users/sync` | * | Sincronizar usuario tras registro en Supabase |

## Experimentos

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/experiments` | RESEARCHER, ADMIN | Crear experimento |
| GET | `/experiments` | * | Listar todos los experimentos (paginado) |
| GET | `/experiments/{id}` | * | Obtener experimento |
| GET | `/experiments/my` | RESEARCHER, ADMIN | Mis experimentos (paginado) |
| PUT | `/experiments/{id}` | RESEARCHER, ADMIN | Actualizar experimento |
| POST | `/experiments/{id}/finish` | RESEARCHER, ADMIN | Marcar experimento como finalizado |
| DELETE | `/experiments/{id}` | RESEARCHER, ADMIN | Eliminar experimento |
| GET | `/experiments/{id}/participants` | RESEARCHER, ADMIN | Inscripciones del experimento (paginado) |
| GET | `/experiments/{id}/responses` | RESEARCHER, ADMIN | Todas las respuestas del experimento |

## Grupos

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/experiments/{id}/groups` | RESEARCHER, ADMIN | Crear grupo |
| GET | `/experiments/{id}/groups` | * | Listar grupos |
| DELETE | `/experiments/groups/{groupId}` | RESEARCHER, ADMIN | Eliminar grupo |

## Fases

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/experiments/{id}/phases` | RESEARCHER, ADMIN | Crear fase |
| GET | `/experiments/{id}/phases` | * | Listar fases |
| DELETE | `/experiments/phases/{phaseId}` | RESEARCHER, ADMIN | Eliminar fase |

## Preguntas

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/questions/phases/{phaseId}` | RESEARCHER, ADMIN | Crear pregunta en una fase |
| GET | `/questions/phases/{phaseId}` | * | Listar preguntas de una fase |
| GET | `/questions/{id}` | * | Obtener pregunta |
| PUT | `/questions/{id}` | RESEARCHER, ADMIN | Actualizar pregunta |
| DELETE | `/questions/{id}` | RESEARCHER, ADMIN | Eliminar pregunta |

## Respuestas

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/questions/enrollments/{enrollmentId}/responses` | PARTICIPANT, ADMIN | Enviar respuesta |
| GET | `/questions/enrollments/{enrollmentId}/responses` | * | Respuestas de una inscripción |
| GET | `/questions/{questionId}/responses` | RESEARCHER, ADMIN | Respuestas a una pregunta |
| GET | `/questions/enrollments/{enrollmentId}/phases/{phaseId}/responses` | * | Respuestas por inscripción y fase |
| DELETE | `/questions/responses/{id}` | RESEARCHER, ADMIN | Eliminar respuesta |

## Participantes

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/participants` | PARTICIPANT, ADMIN | Crear perfil de participante |
| GET | `/participants/me` | PARTICIPANT, ADMIN | Mi perfil |
| GET | `/participants/{id}` | RESEARCHER, ADMIN | Perfil de participante por ID |
| PUT | `/participants/{id}` | PARTICIPANT, ADMIN | Actualizar perfil |

## Inscripciones (Enrollments)

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/enrollments/participants/{participantId}` | PARTICIPANT, ADMIN | Inscribirse en un experimento |
| GET | `/enrollments/participants/{participantId}` | RESEARCHER, ADMIN | Inscripciones de un participante |
| GET | `/enrollments/me` | PARTICIPANT, ADMIN | Mis inscripciones |
| GET | `/enrollments/experiments/{experimentId}` | RESEARCHER, ADMIN | Inscripciones de un experimento |
| PUT | `/enrollments/{enrollmentId}/status` | RESEARCHER, ADMIN | Cambiar estado de inscripción |
| DELETE | `/enrollments/{enrollmentId}` | PARTICIPANT, ADMIN | Retirarse de un experimento |

---

# 10. Seguridad

## Spring Security + Supabase Auth

El sistema utiliza:

* **Supabase Auth** — gestión de usuarios, registro, login, reset de contraseña, OAuth
* **Spring Security** — control de acceso basado en roles (RBAC)
* **SupabaseAuthenticationFilter** — validación del JWT de Supabase en cada petición
* **Sesión stateless** — sin estado en servidor, el token viaja en cada petición

## Control de acceso por rol

Cada endpoint está protegido mediante `@PreAuthorize` con el rol mínimo requerido. Los tres roles del sistema son:

* `ROLE_RESEARCHER` — gestión de experimentos, consulta de datos
* `ROLE_PARTICIPANT` — inscripción y respuesta a cuestionarios
* `ROLE_ADMIN` — acceso total

## CORS

Los orígenes permitidos se configuran en la variable de entorno `CORS_ALLOWED_ORIGINS`. Por defecto admite:

```
http://localhost:3000
http://localhost:5173
```

---

# 11. Configuración del entorno

Variables de entorno necesarias (ver `.env.example`):

```env
DATABASE_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
DATABASE_USERNAME=postgres.xxxxx
DATABASE_PASSWORD=tu_password_de_supabase
SUPABASE_URL=https://xxxxx.supabase.co
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

---

# 12. Arquitectura por capas y principios de diseño

## 12.1 Capas del sistema

```
Controller → Service → Repository → Database
```

| Capa | Responsabilidad |
|---|---|
| Controller | Exponer endpoints REST, validar entrada, delegar al service |
| Service | Lógica de negocio, coordinación entre repositories |
| Repository | Acceso a datos vía Spring Data JPA |
| Model | Entidades JPA, enums de dominio |
| DTO | Contratos de entrada y salida de la API |

## 12.2 Patrones aplicados

* **DTO (Data Transfer Objects)**: separan el modelo interno de persistencia del modelo expuesto por la API
* **Repository Pattern**: abstracción del acceso a datos con Spring Data JPA
* **Service Layer**: encapsula la lógica de negocio; los controllers nunca acceden directamente a los repositories
* **Agregado de dominio**: `Experiment` como agregado principal; `Phase`, `Group` y `Enrollment` son subordinados
* **SRP (Single Responsibility Principle)**: cada clase tiene una única responsabilidad bien definida

## 12.3 Enumeraciones de estado

El sistema usa enums tipados para representar estados y tipos en lugar de strings libres, lo que mejora la seguridad de tipos y la claridad del código.

---

# 13. Estructura del proyecto

```
com.research.experimentplatform
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── ExperimentController.java
│   ├── QuestionController.java
│   ├── ParticipantController.java
│   └── EnrollmentController.java
├── dto/
│   ├── CreateExperimentRequest.java
│   ├── UpdateExperimentRequest.java
│   ├── ExperimentDTO.java
│   ├── CreateGroupRequest.java
│   ├── GroupDTO.java
│   ├── CreatePhaseRequest.java
│   ├── PhaseDTO.java
│   ├── CreateQuestionRequest.java
│   ├── QuestionDTO.java
│   ├── EnrollParticipantRequest.java
│   ├── EnrollmentDTO.java
│   ├── UpdateParticipantRequest.java
│   ├── ParticipantDTO.java
│   ├── SubmitResponseRequest.java
│   └── ResponseDTO.java
├── model/
│   ├── User.java / UserRole.java
│   ├── Participant.java
│   ├── Experiment.java / ExperimentStatus.java / DesignType.java
│   ├── Group.java
│   ├── Phase.java
│   ├── Question.java / QuestionType.java
│   ├── Enrollment.java / EnrollmentStatus.java
│   ├── Response.java
│   ├── Invitation.java / InvitationStatus.java
│   ├── ResearchTeam.java
│   └── TeamMembership.java / TeamRole.java
├── repository/
│   ├── UserRepository.java
│   ├── ParticipantRepository.java
│   ├── ExperimentRepository.java
│   ├── GroupRepository.java
│   ├── PhaseRepository.java
│   ├── QuestionRepository.java
│   ├── EnrollmentRepository.java
│   ├── ResponseRepository.java
│   └── InvitationRepository.java
├── security/
│   └── SupabaseAuthenticationFilter.java
└── service/
    ├── UserService.java
    ├── ParticipantService.java
    ├── ExperimentService.java
    ├── GroupService.java
    ├── PhaseService.java
    ├── QuestionService.java
    ├── EnrollmentService.java
    └── ResponseService.java
```

---

# 14. Pendiente de implementar (TODO)

## TODO-1: Anonimización de datos de participantes

Para cumplir con los principios de privacidad y protección de datos (RGPD), la plataforma debe permitir anonimizar los datos de un participante cuando este abandona un estudio o solicita la eliminación de sus datos.

### Diseño propuesto

La anonimización no eliminará las respuestas (para no invalidar los datos del experimento), sino que **desvinculará la identidad del participante**:

1. El campo `email` del `User` se sustituye por un valor anonimizado: `anon-{uuid}@deleted.local`
2. El campo `supabaseId` se pone a `null` o se reemplaza por un hash irreversible
3. El campo `bio` del `Participant` se vacía
4. La cuenta en Supabase Auth se elimina mediante la API de admin de Supabase
5. Las `Enrollment` y `Response` **se conservan** pero con el participante ya anonimizado

### Endpoint propuesto

```
POST /api/participants/{id}/anonymize
Roles: ADMIN
```

O bien, solicitud de borrado por el propio participante:

```
DELETE /api/participants/me
Roles: PARTICIPANT, ADMIN
```

### Puntos a definir

* ¿Se anonimiza completamente o se elimina la cuenta? (soft delete vs hard anonymization)
* ¿Se conservan las respuestas anónimas para el análisis estadístico?
* ¿Se notifica al investigador cuando un participante solicita anonimización?
* ¿Se aplica automáticamente cuando `EnrollmentStatus` pasa a `WITHDRAWN`?
* Integración con la API de admin de Supabase para borrar el usuario de `auth.users`

### Impacto en el modelo

* Añadir campo `anonymizedAt` en `User` (timestamp de cuándo se anonimizó)
* Añadir campo `deletedAt` en `Participant` para soft delete
* Considerar añadir índice o flag `isAnonymized` en `User`

---

## TODO-2: Exportación de resultados a CSV

Cada experimento debe poder exportar todos sus datos recogidos en un fichero CSV listo para descargar.

Puntos a definir:
* Qué columnas incluye el CSV (participante anónimo o pseudónimo, fase, pregunta, respuesta, fecha, grupo...)
* Si se exporta todo el experimento de una vez o por fase
* Endpoint: `GET /api/experiments/{id}/export/csv`
* Quién puede descargar: solo RESEARCHER y ADMIN
* Formato de fechas y valores numéricos en el CSV
* Si la exportación respeta la anonimización (no exportar emails reales)

---

## TODO-3: Panel de estadísticas

Diseñar qué información debe poder consultar el investigador desde la web.

**Consultas sobre el experimento:**
* ¿Cuántos participantes hay inscritos, activos, completados y retirados?
* ¿Cuántas invitaciones están pendientes, aceptadas o rechazadas?
* ¿Qué porcentaje de participantes ha completado cada fase?

**Consultas sobre respuestas:**
* Distribución de respuestas por pregunta (medias, frecuencias, etc.)
* Comparativa entre grupos (control vs experimental)
* Evolución temporal entre fases (pretest vs postest)

**Consultas sobre participantes:**
* Listado de participantes con su estado y progreso
* Participantes que no han respondido todas las preguntas de una fase

**Impacto en el backend:**
* Definir qué endpoints de agregación se necesitan
* Evaluar si se calculan en backend (queries JPA/SQL) o en el frontend

---

## TODO-4: Gestión de invitaciones

El modelo `Invitation` ya está definido pero los endpoints de invitación no están implementados todavía.

Pendiente:
* `POST /api/experiments/{id}/invitations` — enviar invitación por email
* `GET /api/experiments/{id}/invitations` — listar invitaciones de un experimento
* `POST /api/invitations/{token}/accept` — aceptar invitación (público)
* Integración con servicio de email (ej: Resend, SendGrid, Supabase Email)

---

## TODO-5: URL de producción en CORS

Antes del despliegue, añadir la URL del frontend en producción a la variable `CORS_ALLOWED_ORIGINS` del `.env` del servidor.

---

# 15. Alcance del prototipo actual

La versión actual incluye:

* Autenticación delegada en Supabase Auth
* Sincronización de usuarios
* Creación y gestión de experimentos
* Definición de grupos, fases y preguntas
* Perfiles de participante
* Sistema de inscripciones (Enrollment) con estados y consentimiento
* Envío de respuestas tipadas (texto, número, booleano)
* Control de acceso por rol en todos los endpoints
* Paginación en listados
* Equipos de investigación y membresías (modelo definido)
* Invitaciones (modelo definido, endpoints pendientes)

No incluye aún:

* Anonimización de participantes (diseño especificado en TODO-1)
* Exportación CSV
* Panel de estadísticas
* Envío real de emails de invitación
* Análisis estadístico avanzado
* Integración con dispositivos externos
