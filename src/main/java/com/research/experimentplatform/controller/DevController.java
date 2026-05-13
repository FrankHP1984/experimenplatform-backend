package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.*;
import com.research.experimentplatform.model.*;
import com.research.experimentplatform.repository.*;
import com.research.experimentplatform.service.ExperimentService;
import com.research.experimentplatform.service.GroupService;
import com.research.experimentplatform.service.PhaseService;
import com.research.experimentplatform.service.QuestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevController {

    private final ExperimentService experimentService;
    private final PhaseService phaseService;
    private final GroupService groupService;
    private final QuestionService questionService;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResponseRepository responseRepository;
    private final ExperimentRepository experimentRepository;
    private final PhaseRepository phaseRepository;
    private final QuestionRepository questionRepository;
    private final GroupRepository groupRepository;

    public DevController(ExperimentService experimentService, PhaseService phaseService,
                         GroupService groupService, QuestionService questionService,
                         UserRepository userRepository, ParticipantRepository participantRepository,
                         EnrollmentRepository enrollmentRepository, ResponseRepository responseRepository,
                         ExperimentRepository experimentRepository, PhaseRepository phaseRepository,
                         QuestionRepository questionRepository, GroupRepository groupRepository) {
        this.experimentService = experimentService;
        this.phaseService = phaseService;
        this.groupService = groupService;
        this.questionService = questionService;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.responseRepository = responseRepository;
        this.experimentRepository = experimentRepository;
        this.phaseRepository = phaseRepository;
        this.questionRepository = questionRepository;
        this.groupRepository = groupRepository;
    }

    @PostMapping("/seed")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, String>> seed(Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();

        List<Participant> participantes = crearParticipantes();

        ExperimentDTO exp1 = crearExperimentoPretest(supabaseId);
        ExperimentDTO exp2 = crearExperimentoEntreGrupos(supabaseId);
        ExperimentDTO exp3 = crearExperimentoLongitudinal(supabaseId);

        activarExperimento(exp1.getId());
        activarExperimento(exp2.getId());
        activarExperimento(exp3.getId());

        crearEnrollmentsYRespuestas(exp1.getId(), exp2.getId(), exp3.getId(), participantes);

        return ResponseEntity.ok(Map.of("resultado", "Datos de prueba creados: 3 experimentos activos, 5 participantes, enrollments y respuestas"));
    }

    private List<Participant> crearParticipantes() {
        String[][] datos = {
            { "seed-p01", "marie.curie@ejemplo-seed.com",        "Marie",     "Curie"        },
            { "seed-p02", "albert.einstein@ejemplo-seed.com",    "Albert",    "Einstein"     },
            { "seed-p03", "charles.darwin@ejemplo-seed.com",     "Charles",   "Darwin"       },
            { "seed-p04", "nikola.tesla@ejemplo-seed.com",       "Nikola",    "Tesla"        },
            { "seed-p05", "ada.lovelace@ejemplo-seed.com",       "Ada",       "Lovelace"     },
            { "seed-p06", "isaac.newton@ejemplo-seed.com",       "Isaac",     "Newton"       },
            { "seed-p07", "rosalind.franklin@ejemplo-seed.com",  "Rosalind",  "Franklin"     },
            { "seed-p08", "richard.feynman@ejemplo-seed.com",    "Richard",   "Feynman"      },
            { "seed-p09", "stephen.hawking@ejemplo-seed.com",    "Stephen",   "Hawking"      },
            { "seed-p10", "barbara.mcclintock@ejemplo-seed.com", "Barbara",   "McClintock"   },
            { "seed-p11", "alan.turing@ejemplo-seed.com",        "Alan",      "Turing"       },
            { "seed-p12", "lise.meitner@ejemplo-seed.com",       "Lise",      "Meitner"      },
            { "seed-p13", "carl.sagan@ejemplo-seed.com",         "Carl",      "Sagan"        },
            { "seed-p14", "emmy.noether@ejemplo-seed.com",       "Emmy",      "Noether"      },
            { "seed-p15", "james.watson@ejemplo-seed.com",       "James",     "Watson"       }
        };

        List<Participant> resultado = new java.util.ArrayList<>();
        for (String[] d : datos) {
            User user = userRepository.findByEmail(d[1]).orElseGet(() -> {
                User nuevo = new User(d[0], d[1], UserRole.PARTICIPANT);
                nuevo.setFirstName(d[2]);
                nuevo.setLastName(d[3]);
                return userRepository.save(nuevo);
            });

            Participant participant = participantRepository.findByUserId(user.getId()).orElseGet(() ->
                participantRepository.save(new Participant(user))
            );

            resultado.add(participant);
        }
        return resultado;
    }

    private void activarExperimento(Long experimentId) {
        Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
        exp.setStatus(ExperimentStatus.ACTIVE);
        exp.setStartDate(LocalDateTime.now().minusDays(7));
        exp.setEndDate(LocalDateTime.now().plusDays(30));
        experimentRepository.save(exp);
    }

    private void crearEnrollmentsYRespuestas(Long idPretest, Long idEntreGrupos, Long idLongitudinal,
                                              List<Participant> participantes) {
        Experiment expPretest = experimentRepository.findById(idPretest).orElseThrow();
        Experiment expEntreGrupos = experimentRepository.findById(idEntreGrupos).orElseThrow();
        Experiment expLongitudinal = experimentRepository.findById(idLongitudinal).orElseThrow();

        List<Phase> fasesPretest = phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(idPretest);
        List<Phase> fasesEntreGrupos = phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(idEntreGrupos);
        List<Phase> fasesLongitudinal = phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(idLongitudinal);
        List<Group> gruposEntreGrupos = groupRepository.findByExperimentId(idEntreGrupos);

        for (int i = 0; i < participantes.size(); i++) {
            Participant p = participantes.get(i);

            if (!enrollmentRepository.existsByParticipantIdAndExperimentId(p.getId(), idPretest)) {
                Enrollment e = new Enrollment(p, expPretest, EnrollmentStatus.COMPLETED);
                e = enrollmentRepository.save(e);
                responderFases(e, fasesPretest);
                e.setCompletedAt(LocalDateTime.now().minusDays(2));
                enrollmentRepository.save(e);
            }

            if (!enrollmentRepository.existsByParticipantIdAndExperimentId(p.getId(), idEntreGrupos)) {
                Enrollment e = new Enrollment(p, expEntreGrupos, EnrollmentStatus.ACTIVE);
                if (!gruposEntreGrupos.isEmpty()) {
                    e.setGroup(gruposEntreGrupos.get(i % gruposEntreGrupos.size()));
                }
                e = enrollmentRepository.save(e);
                if (!fasesEntreGrupos.isEmpty()) {
                    responderFases(e, List.of(fasesEntreGrupos.get(0)));
                }
            }

            if (!enrollmentRepository.existsByParticipantIdAndExperimentId(p.getId(), idLongitudinal)) {
                Enrollment e = new Enrollment(p, expLongitudinal, EnrollmentStatus.ACTIVE);
                e = enrollmentRepository.save(e);
                if (!fasesLongitudinal.isEmpty()) {
                    responderFases(e, List.of(fasesLongitudinal.get(0)));
                }
            }
        }
    }

    private void responderFases(Enrollment enrollment, List<Phase> fases) {
        for (Phase fase : fases) {
            List<Question> preguntas = questionRepository.findByPhaseIdOrderByQuestionOrderAsc(fase.getId());
            for (Question pregunta : preguntas) {
                if (responseRepository.existsByEnrollmentIdAndQuestionId(enrollment.getId(), pregunta.getId())) {
                    continue;
                }
                Response respuesta = new Response(enrollment, pregunta);
                rellenarRespuesta(respuesta, pregunta);
                responseRepository.save(respuesta);
            }
        }
    }

    private void rellenarRespuesta(Response respuesta, Question pregunta) {
        QuestionType tipo = pregunta.getType();

        if (tipo == QuestionType.TEXT) {
            String[] textos = {
                "Ha sido una experiencia muy interesante y enriquecedora.",
                "Me ha resultado bastante cómodo y fácil de seguir.",
                "Todo correcto, sin ninguna incidencia reseñable.",
                "Muy buena organización y claridad en las instrucciones.",
                "En general positivo, aunque había momentos de distracción."
            };
            respuesta.setTextValue(textos[(int) (Math.random() * textos.length)]);
        } else if (tipo == QuestionType.SCALE) {
            int min = pregunta.getMinValue() != null ? pregunta.getMinValue() : 1;
            int max = pregunta.getMaxValue() != null ? pregunta.getMaxValue() : 10;
            respuesta.setNumericValue(min + (int) (Math.random() * (max - min + 1)));
        } else if (tipo == QuestionType.BOOLEAN) {
            respuesta.setBooleanValue(Math.random() > 0.4);
        } else if (tipo == QuestionType.NUMBER) {
            respuesta.setNumericValue((int) (Math.random() * 10));
        } else if (tipo == QuestionType.MULTIPLE_CHOICE) {
            if (pregunta.getOptions() != null && !pregunta.getOptions().isEmpty()) {
                respuesta.setTextValue(pregunta.getOptions().get((int) (Math.random() * pregunta.getOptions().size())));
            }
        }
    }

    private ExperimentDTO crearExperimentoPretest(String supabaseId) {
        CreateExperimentRequest req = new CreateExperimentRequest(
                "Efecto del ejercicio en la atención sostenida",
                "Estudio sobre cómo el ejercicio aeróbico moderado afecta la capacidad de atención sostenida en adultos jóvenes.",
                DesignType.PRETEST_POSTTEST,
                null, null,
                "Acepto participar voluntariamente en este estudio y entiendo que mis datos serán tratados de forma anónima.",
                "Gracias por tu participación. Los resultados contribuirán a comprender mejor la relación entre ejercicio y cognición."
        );
        ExperimentDTO exp = experimentService.createExperiment(req, supabaseId);

        PhaseDTO pretest = crearFase(exp.getId(), "Pretest", 1, supabaseId);
        crearPreguntaTexto(pretest.getId(), "¿Cuántas horas duermes habitualmente cada noche?", 1, supabaseId);
        crearPreguntaEscala(pretest.getId(), "¿Cómo valoras tu nivel de energía ahora mismo? (1 = muy bajo, 10 = muy alto)", 2, supabaseId);
        crearPreguntaBooleana(pretest.getId(), "¿Has realizado ejercicio físico en las últimas 24 horas?", 3, supabaseId);

        PhaseDTO postest = crearFase(exp.getId(), "Postest", 2, supabaseId);
        crearPreguntaEscala(postest.getId(), "¿Cómo valoras tu nivel de energía tras la intervención? (1 = muy bajo, 10 = muy alto)", 1, supabaseId);
        crearPreguntaOpcionMultiple(postest.getId(), "¿Cómo describirías tu estado de concentración ahora?",
                List.of("Mucho mejor que antes", "Algo mejor", "Igual", "Peor"), 2, supabaseId);
        crearPreguntaTexto(postest.getId(), "¿Tienes algún comentario adicional sobre tu experiencia?", 3, supabaseId);

        return exp;
    }

    private ExperimentDTO crearExperimentoEntreGrupos(String supabaseId) {
        CreateExperimentRequest req = new CreateExperimentRequest(
                "Impacto del ruido ambiental en el rendimiento cognitivo",
                "Comparación del rendimiento en tareas cognitivas entre participantes expuestos a distintos niveles de ruido.",
                DesignType.BETWEEN_SUBJECTS,
                null, null, null, null
        );
        ExperimentDTO exp = experimentService.createExperiment(req, supabaseId);

        CreateGroupRequest grupoSilencio = new CreateGroupRequest();
        grupoSilencio.setName("Silencio");
        grupoSilencio.setDescription("Condición sin ruido ambiental");
        grupoSilencio.setColor("#06b6d4");
        groupService.createGroup(exp.getId(), grupoSilencio, supabaseId);

        CreateGroupRequest grupoRuido = new CreateGroupRequest();
        grupoRuido.setName("Ruido moderado");
        grupoRuido.setDescription("Condición con ruido de fondo a 65 dB");
        grupoRuido.setColor("#8b5cf6");
        groupService.createGroup(exp.getId(), grupoRuido, supabaseId);

        PhaseDTO fase = crearFase(exp.getId(), "Tarea cognitiva", 1, supabaseId);
        crearPreguntaNumero(fase.getId(), "¿Cuántos errores cometiste en la tarea de vigilancia?", 1, supabaseId);
        crearPreguntaEscala(fase.getId(), "Nivel de dificultad percibida (1 = muy fácil, 10 = muy difícil)", 2, supabaseId);
        crearPreguntaEscala(fase.getId(), "¿Cuánto te ha molestado el entorno durante la tarea? (1 = nada, 10 = muchísimo)", 3, supabaseId);
        crearPreguntaTexto(fase.getId(), "Describe brevemente cómo te has sentido durante la prueba", 4, supabaseId);

        return exp;
    }

    private ExperimentDTO crearExperimentoLongitudinal(String supabaseId) {
        CreateExperimentRequest req = new CreateExperimentRequest(
                "Evolución del bienestar durante el semestre académico",
                "Seguimiento del estado emocional y académico de los estudiantes a lo largo del semestre.",
                DesignType.LONGITUDINAL,
                null, null, null, null
        );
        ExperimentDTO exp = experimentService.createExperiment(req, supabaseId);

        String[] nombres = { "Semana 1", "Semana 5", "Semana 10", "Semana 15" };
        for (int i = 0; i < nombres.length; i++) {
            PhaseDTO fase = crearFase(exp.getId(), nombres[i], i + 1, supabaseId);
            crearPreguntaEscala(fase.getId(), "¿Cómo valoras tu bienestar general esta semana? (1 = muy mal, 10 = excelente)", 1, supabaseId);
            crearPreguntaEscala(fase.getId(), "Nivel de estrés académico (1 = ninguno, 10 = extremo)", 2, supabaseId);
            crearPreguntaBooleana(fase.getId(), "¿Has podido mantener tus hábitos de estudio esta semana?", 3, supabaseId);
        }

        return exp;
    }

    private PhaseDTO crearFase(Long experimentId, String nombre, int orden, String supabaseId) {
        CreatePhaseRequest req = new CreatePhaseRequest();
        req.setName(nombre);
        req.setPhaseOrder(orden);
        return phaseService.createPhase(experimentId, req, supabaseId);
    }

    private void crearPreguntaTexto(Long phaseId, String texto, int orden, String supabaseId) {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setText(texto);
        req.setType(QuestionType.TEXT);
        req.setRequired(true);
        req.setQuestionOrder(orden);
        questionService.createQuestion(phaseId, req, supabaseId);
    }

    private void crearPreguntaEscala(Long phaseId, String texto, int orden, String supabaseId) {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setText(texto);
        req.setType(QuestionType.SCALE);
        req.setMinValue(1);
        req.setMaxValue(10);
        req.setRequired(true);
        req.setQuestionOrder(orden);
        questionService.createQuestion(phaseId, req, supabaseId);
    }

    private void crearPreguntaBooleana(Long phaseId, String texto, int orden, String supabaseId) {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setText(texto);
        req.setType(QuestionType.BOOLEAN);
        req.setRequired(true);
        req.setQuestionOrder(orden);
        questionService.createQuestion(phaseId, req, supabaseId);
    }

    private void crearPreguntaNumero(Long phaseId, String texto, int orden, String supabaseId) {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setText(texto);
        req.setType(QuestionType.NUMBER);
        req.setRequired(true);
        req.setQuestionOrder(orden);
        questionService.createQuestion(phaseId, req, supabaseId);
    }

    private void crearPreguntaOpcionMultiple(Long phaseId, String texto, List<String> opciones, int orden, String supabaseId) {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setText(texto);
        req.setType(QuestionType.MULTIPLE_CHOICE);
        req.setOptions(opciones);
        req.setRequired(true);
        req.setQuestionOrder(orden);
        questionService.createQuestion(phaseId, req, supabaseId);
    }
}
