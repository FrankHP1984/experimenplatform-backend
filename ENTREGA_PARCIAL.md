# Entrega Parcial - Plataforma de Gestión de Experimentos

**Asignatura**: Proyecto de fin de módulo  
**Ciclo**: 2º DAM  
**Fecha**: Marzo 2026

---

## 1. Repositorio

[*(enlace al repositorio)](https://github.com/FrankHP1984/experimentplatform.git)*

---

## 2. Descripción del proyecto

La idea del proyecto es hacer una plataforma web para gestionar experimentos de investigación. Básicamente, un investigador puede crear experimentos, organizarlos en fases y preguntas, y luego los participantes se inscriben y van respondiendo. Algo parecido a lo que usan en universidades para estudios de psicología, educación, etc.

Elegí este tema porque me parecía interesante hacer algo que no fuera la típica tienda online o el CRUD de siempre. Además, tiene bastante chicha a nivel de backend: autenticación, roles, estados que van cambiando, relaciones entre entidades...

La aplicación está pensada como una API REST con Spring Boot. De momento solo está el backend, el frontend lo haré en la siguiente fase.

---

## 3. Tecnologías utilizadas

- **Java 21** con **Spring Boot 4.0.4**
- **Spring Security** para la autenticación (con JWT)
- **Spring Data JPA** para el acceso a datos
- **PostgreSQL** como base de datos en producción
- **H2** para los tests (base de datos en memoria)
- **Maven** como gestor de dependencias
- **Bean Validation** (jakarta.validation) para validar los datos de entrada

La verdad es que al principio empecé con Java 17 pero luego decidí actualizar a Java 21 para poder usar Records en los DTOs, que simplifican bastante el código.

---

## 4. Estructura del proyecto

El proyecto sigue una arquitectura por capas típica de Spring Boot. Lo organicé así:

```
src/main/java/com/research/experimentplatform/
├── config/          → Configuración de seguridad y CORS
├── controller/      → Los endpoints REST (5 controllers)
├── dto/             → Objetos para la comunicación con el cliente (19 DTOs)
├── model/           → Las entidades JPA y los enums (10 clases)
├── repository/      → Interfaces de acceso a datos (7 repos)
├── security/        → Filtro JWT y utilidad para tokens
└── service/         → Lógica de negocio (7 servicios)
```

La idea es que cada capa tenga su responsabilidad clara:
- Los **controllers** solo reciben peticiones HTTP y delegan al servicio correspondiente
- Los **services** tienen toda la lógica de negocio, las validaciones y las transformaciones a DTO
- Los **repositories** se encargan del acceso a base de datos con Spring Data
- Los **DTOs** son lo que se envía al cliente, para no exponer las entidades directamente

Algo que me costó entender al principio fue la diferencia entre el modelo (entidades JPA) y los DTOs. Al final lo entendí como que las entidades son la representación de la base de datos y los DTOs son lo que "ve" el cliente. Así si cambio algo en la base de datos no afecta directamente a la API.

---

## 5. Modelo de datos

Las entidades principales del sistema son:

- **User**: usuarios del sistema (investigadores y participantes). Tiene email, contraseña encriptada y un rol.
- **Experiment**: el experimento en sí. Tiene título, descripción, tipo de diseño (pretest-posttest, between-subjects o longitudinal), fechas y un estado (DRAFT, ACTIVE, PAUSED, FINISHED, CANCELLED).
- **Phase**: cada experimento se divide en fases ordenadas, cada una con sus fechas.
- **Group**: los grupos dentro de un experimento (por ejemplo grupo control y grupo experimental).
- **Question**: las preguntas de cada fase. Pueden ser de varios tipos: texto libre, numérica, escala, opción múltiple o booleana.
- **Participant**: perfil de participante asociado a un usuario.
- **Enrollment**: la inscripción de un participante en un experimento. Tiene su propio estado (PENDING, ACTIVE, COMPLETED, WITHDRAWN).
- **Response**: las respuestas que da cada participante a las preguntas.

Las relaciones principales son:
- Un User puede ser dueño de varios Experiments
- Un Experiment tiene varias Phases, Groups y Enrollments
- Una Phase tiene varias Questions
- Un Participant tiene varios Enrollments
- Un Enrollment tiene varias Responses

He configurado las eliminaciones en cascada en JPA para que si se borra un experimento se borren también sus fases, grupos, inscripciones, preguntas y respuestas asociadas. Esto me dio algún quebradero de cabeza al principio porque no tenía claro cómo funcionaba el `CascadeType.ALL` con `orphanRemoval`.

---

## 6. Lo que está hecho (funcionalidad implementada)

### 6.1 Autenticación y seguridad

Implementé un sistema de autenticación basado en JWT. El flujo es:

1. El usuario se registra en `/api/auth/register` con email, contraseña y rol
2. Hace login en `/api/auth/login` y recibe un token JWT
3. En cada petición siguiente, manda el token en el header `Authorization: Bearer <token>`
4. Un filtro (`JwtAuthenticationFilter`) intercepta todas las peticiones, valida el token y si es correcto deja pasar la petición

El token JWT contiene el email, el rol y el id del usuario. La contraseña se guarda encriptada con BCrypt. Lo de meter el userId en el token lo hice porque al principio solo tenía el email y luego para sacar el id del usuario tenía que hacer otra consulta a la base de datos cada vez, que era un poco absurdo.

También configuré CORS para que el frontend pueda hacer peticiones desde localhost (tanto para React como para Vite).

### 6.2 Gestión de experimentos

El CRUD completo de experimentos está hecho:
- Crear, obtener, actualizar y eliminar experimentos
- Listar todos los experimentos con **paginación** (uso `Pageable` de Spring Data)
- Listar los experimentos de un investigador concreto (también paginado)

Lo más interesante aquí es la **validación de transiciones de estado**. No se puede pasar de cualquier estado a cualquier otro. Por ejemplo, un experimento FINISHED no puede volver a DRAFT. Para controlar esto hice un mapa con las transiciones válidas y una función que lo comprueba antes de cambiar el estado.

La actualización es parcial: solo se modifican los campos que vienen en el request. Los que son null se ignoran. Esto lo hice para que el cliente pueda actualizar solo el título sin tener que mandar todos los demás campos.

### 6.3 Fases, grupos y preguntas

- Se pueden crear fases dentro de un experimento, con un orden y fechas
- Se pueden crear grupos dentro de un experimento (grupo control, experimental, etc.)
- Se pueden crear preguntas dentro de una fase, con diferentes tipos (TEXT, NUMBER, SCALE, MULTIPLE_CHOICE, BOOLEAN)

Las fases se devuelven ordenadas por el campo `phaseOrder`. Las preguntas también tienen un campo `questionOrder`.

### 6.4 Inscripciones (enrollments)

Esta parte fue la que más me costó. El flujo es:

1. Un participante se inscribe en un experimento activo
2. Se le puede asignar a un grupo (opcional)
3. El participante puede ir respondiendo preguntas mientras su inscripción esté ACTIVE
4. Al terminar se marca como COMPLETED, o si abandona como WITHDRAWN

Tiene validaciones como:
- No te puedes inscribir dos veces en el mismo experimento
- Solo te puedes inscribir en experimentos que estén en estado ACTIVE
- Si asignas un grupo, tiene que pertenecer al experimento correcto

También tiene su propia validación de transiciones de estado, como los experimentos.

Un problema que encontré es que si dos personas intentan inscribirse exactamente a la vez, la validación de duplicados no es suficiente porque entre que uno comprueba y guarda puede colarse el otro. Lo solucioné con una constraint UNIQUE en la base de datos y capturando la excepción `DataIntegrityViolationException` si salta. No es la solución más elegante pero funciona y es simple.

Las listas de inscripciones están **paginadas** porque potencialmente puede haber muchas.

### 6.5 Respuestas

Los participantes pueden enviar respuestas a las preguntas. Antes de guardar una respuesta se valida:
- Que la inscripción esté en estado ACTIVE
- Que la pregunta pertenezca al mismo experimento que la inscripción (para que no puedas responder preguntas de otro experimento)
- Que el tipo de respuesta sea coherente con el tipo de pregunta

### 6.6 DTOs con Records de Java 21

Los DTOs de entrada (requests) los he convertido a Records de Java. Un Record es básicamente una clase inmutable que te genera automáticamente el constructor, los getters, equals, hashCode y toString. Queda mucho más limpio que una clase con todos los getters y setters:

```java
public record LoginRequest(
    @NotBlank(message = "Email is required") String email,
    @NotBlank(message = "Password is required") String password
) {}
```

Esto solo lo hice con los DTOs de request porque son inmutables (los creas y no los modificas). Los DTOs de respuesta (ExperimentDTO, EnrollmentDTO, etc.) también los tengo que pasar a Records, es algo que tengo pendiente.

---

## 7. Endpoints de la API

Un resumen de los endpoints que hay implementados:

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/register` | Registro de usuario |
| POST | `/api/auth/login` | Login (devuelve JWT) |
| POST | `/api/experiments` | Crear experimento |
| GET | `/api/experiments` | Listar experimentos (paginado) |
| GET | `/api/experiments/{id}` | Obtener experimento |
| PUT | `/api/experiments/{id}` | Actualizar experimento |
| DELETE | `/api/experiments/{id}` | Eliminar experimento |
| POST | `/api/experiments/{id}/groups` | Crear grupo |
| GET | `/api/experiments/{id}/groups` | Listar grupos |
| DELETE | `/api/experiments/groups/{id}` | Eliminar grupo |
| POST | `/api/experiments/{id}/phases` | Crear fase |
| GET | `/api/experiments/{id}/phases` | Listar fases |
| DELETE | `/api/experiments/phases/{id}` | Eliminar fase |
| POST | `/api/enrollments/participants/{id}` | Inscribirse en experimento |
| GET | `/api/enrollments/participants/{id}` | Inscripciones de un participante |
| GET | `/api/enrollments/me` | Mis inscripciones |
| GET | `/api/enrollments/experiments/{id}` | Inscripciones de un experimento |
| PUT | `/api/enrollments/{id}/status` | Cambiar estado inscripción |
| DELETE | `/api/enrollments/{id}` | Retirar inscripción |
| POST | `/api/questions/phases/{id}` | Crear pregunta |
| GET | `/api/questions/phases/{id}` | Preguntas de una fase |
| GET | `/api/questions/{id}` | Obtener pregunta |
| PUT | `/api/questions/{id}` | Actualizar pregunta |
| DELETE | `/api/questions/{id}` | Eliminar pregunta |
| POST | `/api/questions/enrollments/{id}/responses` | Enviar respuesta |
| GET | `/api/questions/enrollments/{id}/responses` | Respuestas de una inscripción |
| GET | `/api/questions/{id}/responses` | Respuestas de una pregunta |
| DELETE | `/api/questions/responses/{id}` | Eliminar respuesta |

Son 28 endpoints en total. Todos requieren autenticación excepto el registro y el login.

---

## 8. Dificultades que me he encontrado

- **Entender JWT**: al principio me costó entender cómo funcionaba todo el flujo del token. Especialmente la parte del filtro que intercepta las peticiones y cómo se guarda la autenticación en el SecurityContext de Spring.

- **Las relaciones en JPA**: configurar bien los `@OneToMany`, `@ManyToOne`, el `fetch = LAZY`, los cascades... Todo esto me llevó bastante tiempo. Sobre todo cuando empecé a borrar entidades y me saltaban errores de integridad referencial porque no tenía bien los cascades.

- **La concurrencia en inscripciones**: el tema del race condition me pareció interesante. Nunca había pensado en qué pasa si dos peticiones llegan exactamente a la vez. La solución de capturar la excepción de la constraint UNIQUE funciona, aunque seguramente haya formas más sofisticadas de hacerlo.

- **Paginación**: no sabía que Spring Data te lo daba casi gratis con `Pageable`. Solo hay que poner el parámetro en el repository y en el controller, y Spring se encarga de parsear los parámetros `page`, `size` y `sort` de la URL.

---

## 9. Lo que queda por hacer

Cosas que tengo pendientes para la entrega final:

- **Tests**: tengo que hacer tests unitarios de los servicios y tests de integración de los controllers. Ya tengo H2 como dependencia de test así que la base de datos en memoria está preparada.

- **Manejo de errores global**: ahora mismo si algo falla devuelve un 500 genérico. Tengo que hacer un `@ControllerAdvice` para que devuelva errores más descriptivos con los códigos HTTP correctos (400, 404, 409, etc.).

- **Convertir los DTOs de respuesta a Records**: me faltan los DTOs como ExperimentDTO, EnrollmentDTO, etc. Los de request ya están convertidos.

- **Algunos casos edge que tengo apuntados**: por ejemplo qué pasa si cambias el tipo de diseño de un experimento que ya tiene datos, o si se crea una pregunta MULTIPLE_CHOICE sin opciones, o el tema de las mayúsculas/minúsculas en los emails.

- **Frontend**: la interfaz de usuario. Estoy pensando en hacerla con React, pero todavía no he empezado.

- **README del proyecto**: documentar cómo arrancar el proyecto, configurar la base de datos, etc.

---

## 10. Conclusiones de esta entrega parcial

A día de hoy tengo todo el backend funcional con las operaciones principales: autenticación con JWT, gestión completa de experimentos con sus fases, grupos y preguntas, sistema de inscripciones con control de estados, y envío de respuestas con validaciones.

Lo que más me ha aportado ha sido aprender a estructurar un proyecto real con Spring Boot, separando bien las capas y entendiendo para qué sirve cada una. También el tema de seguridad con JWT, que al principio parecía complicado pero una vez entiendes el flujo tiene bastante lógica.

El proyecto está en un punto donde el backend funciona y se puede probar con Postman o similar. Lo que queda es pulirlo (tests, errores, edge cases) y hacer el frontend.
