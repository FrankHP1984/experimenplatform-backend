# AUDITORÍA COMPLETA DEL PROYECTO - ExperimentPlatform
## Fecha: 25/03/2026
## Versión: 1.0

---

# ÍNDICE

1. Resumen ejecutivo
2. Auditoría de principios arquitectónicos
3. Auditoría SOLID
4. Auditoría de código humano
5. Auditoría de seguridad
6. Auditoría de README
7. Fallos críticos detectados
8. Elementos no contemplados
9. Casos edge no cubiertos
10. Recomendaciones priorizadas

---

# 1. RESUMEN EJECUTIVO

## Estado general: NECESITA CORRECCIONES

El proyecto presenta una base arquitectónica sólida con separación clara de capas,
pero tiene **fallos críticos** que deben resolverse antes de cualquier despliegue.
Los principios de diseño se cumplen parcialmente. Se han detectado problemas de
seguridad, ausencia de manejo de errores global, y placeholders sin implementar.

### Archivos auditados: 40 archivos Java
### Hallazgos críticos: 7
### Hallazgos importantes: 12
### Hallazgos menores: 8

---

# 2. AUDITORÍA DE PRINCIPIOS ARQUITECTÓNICOS

## 2.1 Flujo Controller → Service → Repository → Database

### Veredicto: CUMPLE PARCIALMENTE

**Lo que cumple:**
- Todos los controllers delegan al service correspondiente
- Los services coordinan repositorios y aplican lógica de negocio
- Los repositories solo contienen acceso a datos (Spring Data JPA)
- Las entidades representan correctamente el modelo de persistencia
- Los DTOs separan el modelo interno de la API

**Lo que NO cumple:**

### HALLAZGO CRÍTICO #1: Controllers con lógica de negocio
- ParticipantController.getMyEnrollments() (línea 67-71) contiene lógica de
  orquestación: primero busca el participante por userId, luego busca sus enrollments.
  Esta lógica debe estar en el service.

  `java
  // ACTUAL (controller tiene lógica)
  ParticipantDTO participant = participantService.getParticipantByUserId(userId);
  List<EnrollmentDTO> enrollments = participantService.getParticipantEnrollments(participant.getId());

  // DEBERÍA SER (un solo método en service)
  List<EnrollmentDTO> enrollments = participantService.getMyEnrollments(userId);
  `

### HALLAZGO CRÍTICO #2: extractUserIdFromAuthentication() es un placeholder hardcodeado
- ExperimentController.extractUserIdFromAuthentication() retorna eturn 1L;
- ParticipantController.extractUserIdFromAuthentication() retorna eturn 1L;
- **Esto rompe completamente la seguridad JWT**: cualquier petición autenticada
  se ejecuta como si fuera el usuario con ID 1.
- El método está duplicado en dos controllers, violando DRY.

## 2.2 Responsabilidades por capa

### Controllers
| Archivo | Solo HTTP/DTOs | Sin lógica negocio | Delega a service |
|---------|:-:|:-:|:-:|
| AuthController | SI | SI | SI |
| ExperimentController | SI | PARCIAL (*) | SI |
| ParticipantController | SI | NO (**) | SI |
| QuestionController | SI | SI | SI |

(*) Tiene placeholder de autenticación
(**) Orquesta dos llamadas a service en getMyEnrollments()

### Services
| Archivo | Lógica negocio | Validaciones | DTO↔Entity |
|---------|:-:|:-:|:-:|
| UserService | SI | SI | SI |
| ExperimentService | SI | SI | SI |
| GroupService | SI | SI | SI |
| PhaseService | SI | SI | SI |
| ParticipantService | SI | SI | SI |
| QuestionService | SI | SI | SI |
| ResponseService | SI | SI | SI |

### Repositories
| Archivo | Solo acceso datos | Sin lógica negocio |
|---------|:-:|:-:|
| UserRepository | SI | SI |
| ExperimentRepository | SI | SI |
| GroupRepository | SI | SI |
| PhaseRepository | SI | SI |
| ParticipantRepository | SI | SI |
| EnrollmentRepository | SI | SI |
| QuestionRepository | SI | SI |
| ResponseRepository | SI | SI |

## 2.3 Patrón Strategy y Factory

### HALLAZGO IMPORTANTE #1: Strategy y Factory NO están implementados
Los principios del proyecto especifican que se deben usar:
- **Strategy Pattern**: para tipos de diseño experimental (PretestPosttest, BetweenSubjects, Longitudinal)
- **Factory Pattern**: para selección dinámica de estrategia

Actualmente, DesignType es solo un enum sin comportamiento asociado.
No existe ninguna interfaz de estrategia ni factory.

**Estado**: PENDIENTE DE IMPLEMENTAR
**Impacto**: El sistema no diferencia comportamiento según el tipo de diseño experimental.

---

# 3. AUDITORÍA SOLID

## 3.1 Single Responsibility Principle (SRP)

### Veredicto: CUMPLE MAYORMENTE

**Cumple:**
- Cada controller tiene responsabilidad clara de una entidad/módulo
- Cada service encapsula la lógica de su dominio
- Cada repository gestiona una sola entidad
- DTOs separados para request/response

**No cumple:**

### HALLAZGO IMPORTANTE #2: ParticipantService tiene demasiadas responsabilidades
ParticipantService gestiona tanto Participants como Enrollments.
Tiene 5 dependencias inyectadas, lo cual es señal de que hace demasiado.

Responsabilidades mezcladas:
- CRUD de Participant
- Lógica de enrollment
- Lógica de withdrawal
- Conteo de enrollments activos

**Recomendación**: Extraer EnrollmentService como servicio separado.

### HALLAZGO MENOR #1: QuestionController gestiona Questions Y Responses
QuestionController inyecta tanto QuestionService como ResponseService.
Los endpoints de responses (/enrollments/{id}/responses) no pertenecen
conceptualmente al recurso /api/questions.

**Recomendación**: Considerar un ResponseController o reubicar bajo /api/responses.

## 3.2 Open/Closed Principle (OCP)

### Veredicto: CUMPLE PARCIALMENTE

**Cumple:**
- Uso de enums extensibles para tipos y estados
- Repositorios extienden JpaRepository (abiertos a extensión)
- Servicios se pueden extender sin modificar controllers

**No cumple:**
- Sin Strategy/Factory, añadir un nuevo DesignType requiere modificar
  el código existente en lugar de extender.
- ResponseService.validateAndSetResponseValue() usa switch, añadir un
  nuevo QuestionType requiere modificar este método.

## 3.3 Liskov Substitution Principle (LSP)

### Veredicto: NO APLICA AÚN
No hay interfaces de servicio ni abstracciones sustituibles implementadas.
Los services son clases concretas directamente inyectadas.

## 3.4 Interface Segregation Principle (ISP)

### Veredicto: CUMPLE
Los repositories definen interfaces pequeñas y específicas.
No se detectan interfaces sobredimensionadas.

## 3.5 Dependency Inversion Principle (DIP)

### Veredicto: CUMPLE PARCIALMENTE

**Cumple:**
- Inyección de dependencias vía constructor en todos los componentes
- Spring gestiona el ciclo de vida

**No cumple:**
### HALLAZGO IMPORTANTE #3: Services dependen de implementaciones concretas, no de abstracciones
Todos los services se inyectan como clases concretas, no interfaces.
Ejemplo: ExperimentService se inyecta directamente, no hay interfaz ExperimentService.

Esto viola DIP estrictamente, aunque en el contexto de Spring Boot con
principio de mínima ingeniería, es aceptable mientras no haya necesidad de
múltiples implementaciones. Sin embargo, dificulta el testing con mocks.

---

# 4. AUDITORÍA DE CÓDIGO HUMANO

## 4.1 Claridad y legibilidad

### Veredicto: CUMPLE MAYORMENTE

**Cumple:**
- Nombres descriptivos y consistentes
- Estructura predecible
- Sin comentarios innecesarios (0 comentarios en todo el proyecto)
- Código directo y sin abstracciones artificiales

**Observaciones:**

### HALLAZGO MENOR #2: Imports con wildcard
Varios archivos usan imports con wildcard que dificultan la legibilidad:
- ParticipantService: import com.research.experimentplatform.model.*;
- ParticipantService: import com.research.experimentplatform.repository.*;
- ParticipantController: import com.research.experimentplatform.dto.*;
- ExperimentController: import com.research.experimentplatform.dto.*;
- QuestionController: import com.research.experimentplatform.dto.*;

Un desarrollador experimentado generalmente prefiere imports explícitos.

### HALLAZGO MENOR #3: Import no utilizado
ResponseService.java línea 7: import com.research.experimentplatform.model.QuestionType;
Import sin usar. Código muerto que levanta sospechas de generación automática.

## 4.2 Consistencia

### Veredicto: CUMPLE

- Nomenclatura consistente (entidad, entidadDTO, CreateEntidadRequest)
- Estructura de paquetes predecible
- Patrón de conversión DTO consistente (convertToDTO en cada service)
- Estilo de código uniforme

## 4.3 Mínima ingeniería

### Veredicto: CUMPLE
- No hay capas innecesarias
- No hay abstracciones prematuras
- Sin configuraciones complejas
- Soluciones directas

---

# 5. AUDITORÍA DE SEGURIDAD

## 5.1 Autenticación JWT

### HALLAZGO CRÍTICO #3: extractUserIdFromAuthentication() hardcodeado
Como se mencionó en 2.1, el método retorna siempre 1L.
**Todo el sistema de ownership y autorización está roto.**

Consecuencias:
- Cualquier usuario autenticado puede modificar/eliminar CUALQUIER experimento
- Cualquier usuario se registra como si fuera el user 1
- No hay verificación de propiedad en ningún endpoint

### HALLAZGO CRÍTICO #4: No hay autorización por roles
SecurityConfig solo diferencia entre rutas públicas (/api/auth/**) y
autenticadas (todo lo demás). No hay control de roles.

Problemas:
- Un PARTICIPANT puede crear experimentos (solo debería RESEARCHER)
- Un RESEARCHER puede inscribirse como participante
- No se valida el rol del usuario en ningún endpoint
- Los endpoints de administración (DELETE, PUT) son accesibles para cualquier rol

### HALLAZGO CRÍTICO #5: No hay verificación de ownership
Los endpoints de modificación/eliminación no verifican que el usuario sea el dueño:
- PUT /api/experiments/{id} - cualquiera puede editar cualquier experimento
- DELETE /api/experiments/{id} - cualquiera puede eliminar
- DELETE /api/experiments/groups/{id} - cualquiera puede eliminar grupos
- DELETE /api/experiments/phases/{id} - cualquiera puede eliminar fases
- DELETE /api/questions/{id} - cualquiera puede eliminar preguntas
- PUT /api/participants/{id} - cualquiera puede editar cualquier participante
- PUT /api/participants/enrollments/{id}/status - cualquiera puede cambiar estado

### HALLAZGO IMPORTANTE #4: JWT almacena email como subject pero el sistema necesita userId
JwtAuthenticationFilter extrae email y role del token, pero los controllers
necesitan userId. No hay forma de obtener el userId del token actual.

El Authentication principal contiene el email (String), no el User entity ni el ID.

### HALLAZGO IMPORTANTE #5: No hay rate limiting ni protección contra brute force
El endpoint /api/auth/login no tiene protección contra ataques de fuerza bruta.

### HALLAZGO MENOR #4: JWT secret por defecto en código
JwtUtil línea 19 tiene un secret por defecto en la anotación @Value:
defaultSecretKeyForDevelopmentPurposesOnlyChangeInProduction
Si application.properties no define el secret, se usa este valor predecible.

## 5.2 Validación de entrada

### HALLAZGO IMPORTANTE #6: Validación inconsistente en DTOs
- RegisterRequest y LoginRequest: tienen @NotBlank y @Email (correcto)
- CreateExperimentRequest: tiene @NotBlank y @NotNull (correcto)
- UpdateExperimentRequest: NO tiene validaciones (todos los campos opcionales, correcto)
- UpdateParticipantRequest: NO tiene ninguna validación, ni siquiera @Size para bio
- EnrollParticipantRequest: solo @NotNull en experimentId (correcto)
- SubmitResponseRequest: solo @NotNull en questionId (correcto)

Falta validación de longitud máxima en campos de texto que podrían recibir
payloads grandes (bio, description, textValue de responses).

---

# 6. AUDITORÍA DE README

## Veredicto: README VACÍO

El archivo eadme.md está completamente vacío (solo contiene una línea en blanco).
No hay documentación del proyecto.

### HALLAZGO IMPORTANTE #7: README sin contenido
Un proyecto debe tener al mínimo:
- Descripción del proyecto
- Requisitos (Java 17, Maven, PostgreSQL)
- Instrucciones de instalación y configuración
- Cómo ejecutar el proyecto
- Estructura del proyecto
- Endpoints disponibles
- Variables de entorno necesarias (jwt.secret)

---

# 7. FALLOS CRÍTICOS DETECTADOS

## FC-01: Sistema de autenticación de usuario roto (CRÍTICO)
**Archivo**: ExperimentController.java:104-106, ParticipantController.java:88-90
**Problema**: extractUserIdFromAuthentication() retorna siempre 1L
**Impacto**: Todo el sistema de propiedad y autorización está deshabilitado
**Solución**: Implementar extracción real del userId desde el JWT token.
Requiere:
1. Incluir userId en los claims del JWT
2. O buscar el User por email (que sí está en el token)
3. Centralizar este método (no duplicar en cada controller)

## FC-02: Sin control de acceso por roles (CRÍTICO)
**Archivo**: SecurityConfig.java:28-30
**Problema**: Solo hay uthenticated(), sin hasRole() ni hasAuthority()
**Impacto**: Cualquier usuario autenticado accede a todo
**Solución**: Configurar roles por ruta o usar @PreAuthorize en controllers

## FC-03: Sin verificación de ownership (CRÍTICO)
**Archivo**: Todos los services de escritura
**Problema**: No se verifica que el usuario sea propietario antes de modificar/eliminar
**Impacto**: Cualquier usuario puede modificar datos de otros
**Solución**: Añadir validación de ownership en los services

## FC-04: No hay Global Exception Handler (CRÍTICO)
**Archivo**: No existe
**Problema**: Las IllegalArgumentException se propagan como 500 Internal Server Error
**Impacto**: El cliente recibe errores genéricos sin información útil.
Stack traces pueden filtrarse en la respuesta HTTP.
**Solución**: Crear @ControllerAdvice con @ExceptionHandler

## FC-05: No hay configuración de base de datos (CRÍTICO)
**Archivo**: application.properties
**Problema**: Solo tiene spring.application.name y configuración JWT.
No hay configuración de datasource PostgreSQL.
**Impacto**: La aplicación NO puede arrancar. No hay URL, usuario ni contraseña de BD.
**Solución**: Añadir spring.datasource.url, username, password, y spring.jpa.hibernate.ddl-auto

## FC-06: Tabla "groups" es palabra reservada en SQL (CRÍTICO)
**Archivo**: Group.java:6 - @Table(name = "groups")
**Problema**: "groups" es palabra reservada en la mayoría de SGBD (PostgreSQL incluido)
**Impacto**: Hibernate fallará al intentar crear la tabla o ejecutar queries
**Solución**: Cambiar a @Table(name = "experiment_groups") o usar backticks

## FC-07: No hay cascada de eliminación coherente (CRÍTICO)
**Archivo**: Experiment.java, Phase.java
**Problema**: Experiment tiene cascade = CascadeType.ALL, orphanRemoval = true
para phases y groups, pero NO para enrollments.
Eliminar un experiment no elimina sus enrollments, lo que causará
ConstraintViolationException.
Igualmente, eliminar una phase no elimina sus questions, y eliminar
una question no elimina sus responses (no hay cascada definida).
**Impacto**: Operaciones DELETE fallarán con errores de integridad referencial
**Solución**: Definir cascada coherente o implementar soft-delete

---

# 8. ELEMENTOS NO CONTEMPLADOS

## 8.1 Infraestructura ausente

### EN-01: No hay Global Exception Handler (@ControllerAdvice)
Sin manejo centralizado de excepciones, todas las IllegalArgumentException
y RuntimeException llegan al cliente como 500 con stack trace.
Se necesita:
`
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) → 400 Bad Request
    @ExceptionHandler(EntityNotFoundException.class) → 404 Not Found
    @ExceptionHandler(AccessDeniedException.class) → 403 Forbidden
    @ExceptionHandler(Exception.class) → 500 Internal Server Error
}
`

### EN-02: No hay configuración de CORS
Si el frontend (React) se ejecuta en un dominio/puerto diferente,
todas las peticiones serán rechazadas por política CORS del navegador.

### EN-03: No hay paginación
getAllExperiments() retorna TODOS los experimentos sin paginación.
getResponsesByExperiment() retorna TODAS las respuestas.
Con datos reales, esto causará problemas de rendimiento y memoria.

### EN-04: No hay logging estructurado
Solo hay un logger.error en JwtAuthenticationFilter.
No hay logs de auditoría para operaciones importantes
(crear experimento, enrollment, submit response).

### EN-05: No hay profiles de Spring
No existe pplication-dev.properties ni pplication-prod.properties.
La misma configuración se usa para desarrollo y producción.

### EN-06: No hay tests
El único test es ExperimentplatformApplicationTests.contextLoads() que
probablemente falla porque no hay configuración de base de datos para tests.

## 8.2 Lógica de negocio ausente

### EN-07: No hay validación de transiciones de estado de Experiment
ExperimentService.updateExperiment() permite cambiar el status libremente.
No hay máquina de estados: se puede pasar de FINISHED a DRAFT.
Transiciones válidas deberían ser: DRAFT → ACTIVE → FINISHED

### EN-08: No hay validación de transiciones de estado de Enrollment
ParticipantService.updateEnrollmentStatus() permite cualquier transición.
Se puede pasar de WITHDRAWN a ACTIVE, de COMPLETED a PENDING, etc.

### EN-09: No se valida que la question pertenezca al experiment del enrollment
ResponseService.submitResponse() no verifica que la question esté en una phase
del experiment al que pertenece el enrollment. Un participante podría responder
preguntas de un experimento en el que no está inscrito.

### EN-10: No hay límite de participantes por experimento
No existe campo maxParticipants en Experiment ni validación de capacidad.

### EN-11: No se valida el estado del enrollment al responder
ResponseService.submitResponse() no verifica que el enrollment esté ACTIVE.
Un participante con enrollment WITHDRAWN podría seguir enviando respuestas.

### EN-12: No hay validación de fechas
- No se valida que endDate > startDate en Experiment ni Phase
- No se valida que las fechas de las phases estén dentro del rango del experiment

---

# 9. CASOS EDGE NO CUBIERTOS

## CE-01: Eliminación en cascada de datos relacionados
Si se elimina un Experiment:
- Phases y Groups se eliminan (cascade)
- Enrollments NO se eliminan → ConstraintViolationException
- Questions de las phases NO se eliminan → ConstraintViolationException
- Responses NO se eliminan → ConstraintViolationException

## CE-02: Usuario con múltiples roles
El sistema asume un único rol por usuario (UserRole enum).
Un investigador que también participa en otros experimentos no está contemplado.

## CE-03: Concurrencia en enrollment
Si dos peticiones de enrollment llegan simultáneamente para el mismo
participante y experimento, podrían crearse dos enrollments antes de
que se active la constraint UNIQUE. El manejo de DataIntegrityViolationException
no está implementado.

## CE-04: Respuestas parciales
No hay forma de saber si un participante ha completado todas las preguntas
obligatorias de una phase. No hay endpoint para verificar completitud.

## CE-05: Cambio de DesignType después de tener datos
Si un experimento tiene participantes y respuestas, cambiar el DesignType
podría invalidar los datos existentes. No hay protección contra esto.

## CE-06: Question con options vacío para MULTIPLE_CHOICE
CreateQuestionRequest no valida que options sea no-nulo cuando
	ype = MULTIPLE_CHOICE. Se podría crear una pregunta de opción múltiple
sin opciones.

## CE-07: Eliminación de Group con Enrollments asignados
Si se elimina un Group que tiene enrollments asignados, los enrollments
quedarán con group_id apuntando a un registro inexistente (o fallará
por constraint).

## CE-08: Email case sensitivity
UserRepository.findByEmail() hace match exacto. "User@email.com" y
"user@email.com" serían usuarios diferentes. Se debería normalizar a lowercase.

---

# 10. RECOMENDACIONES PRIORIZADAS

## Prioridad 1 - CRÍTICAS (bloquean despliegue)

1. **Implementar extractUserIdFromAuthentication()** correctamente
   - Incluir userId en JWT claims, o buscar user por email
   - Centralizar en una clase utility o base controller

2. **Añadir configuración de base de datos** en application.properties
   - spring.datasource.url, username, password
   - spring.jpa.hibernate.ddl-auto=validate (producción)

3. **Crear GlobalExceptionHandler** (@ControllerAdvice)
   - Mapear excepciones a HTTP status codes apropiados
   - No exponer stack traces al cliente

4. **Renombrar tabla "groups"** a "experiment_groups" (palabra reservada SQL)

5. **Implementar autorización por roles** (RESEARCHER vs PARTICIPANT)
   - RESEARCHER: crear/editar/eliminar experimentos, preguntas
   - PARTICIPANT: inscribirse, responder preguntas, ver sus datos

6. **Implementar verificación de ownership** en operaciones de escritura

7. **Corregir cascada de eliminación** o implementar restricciones coherentes

## Prioridad 2 - IMPORTANTES (funcionalidad incorrecta)

8. **Implementar Strategy/Factory** para DesignType (requisito arquitectónico)
9. **Validar transiciones de estado** de Experiment y Enrollment
10. **Validar que question pertenezca al experiment** del enrollment en ResponseService
11. **Validar estado ACTIVE del enrollment** antes de aceptar respuestas
12. **Extraer EnrollmentService** de ParticipantService (SRP)
13. **Mover lógica de ParticipantController.getMyEnrollments()** al service
14. **Añadir configuración CORS**
15. **Añadir paginación** a endpoints que retornan listas

## Prioridad 3 - MENORES (calidad de código)

16. Reemplazar imports wildcard por imports explícitos
17. Eliminar import no usado en ResponseService
18. Añadir validación de longitud a campos de texto en DTOs
19. Normalizar email a lowercase en registro y login
20. Crear README con documentación del proyecto
21. Crear profiles de Spring (dev, prod)
22. Añadir tests unitarios e integración

---

# RESUMEN FINAL

| Categoría | Estado |
|-----------|--------|
| Arquitectura por capas | CUMPLE (con correcciones menores) |
| Separación Controller/Service | CUMPLE PARCIALMENTE |
| Patrón DTO | CUMPLE |
| Patrón Repository | CUMPLE |
| Patrón Strategy/Factory | NO IMPLEMENTADO |
| SRP | CUMPLE PARCIALMENTE |
| OCP | CUMPLE PARCIALMENTE |
| DIP | CUMPLE PARCIALMENTE |
| Código humano | CUMPLE |
| Mínima ingeniería | CUMPLE |
| Comentarios | CUMPLE |
| Seguridad JWT | ROTO (placeholder) |
| Autorización | NO IMPLEMENTADA |
| Manejo de errores | NO IMPLEMENTADO |
| Configuración BD | AUSENTE |
| Tests | AUSENTES |
| README | VACÍO |

**El proyecto tiene una base arquitectónica sólida pero requiere correcciones
críticas antes de ser funcional. Las 7 correcciones de Prioridad 1 son
bloqueantes para cualquier despliegue o prueba real.**

---
Documento generado como parte de la auditoría interna del proyecto.
Este archivo NO debe subirse al repositorio (incluido en .gitignore).
