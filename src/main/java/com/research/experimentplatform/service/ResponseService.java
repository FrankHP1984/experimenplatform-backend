package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.ResponseDTO;
import com.research.experimentplatform.dto.SubmitResponseRequest;
import com.research.experimentplatform.model.DesignType;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Phase;
import com.research.experimentplatform.model.Question;
import com.research.experimentplatform.model.Response;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.PhaseRepository;
import com.research.experimentplatform.repository.QuestionRepository;
import com.research.experimentplatform.repository.ResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuestionRepository questionRepository;
    private final PhaseRepository phaseRepository;

    public ResponseService(ResponseRepository responseRepository,
                          EnrollmentRepository enrollmentRepository,
                          QuestionRepository questionRepository,
                          PhaseRepository phaseRepository) {
        this.responseRepository = responseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.questionRepository = questionRepository;
        this.phaseRepository = phaseRepository;
    }

    @Transactional
    public ResponseDTO submitResponse(Long enrollmentId, SubmitResponseRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Enrollment is not active");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        Long enrollmentExperimentId = enrollment.getExperiment().getId();
        Long questionExperimentId = question.getPhase().getExperiment().getId();
        if (!enrollmentExperimentId.equals(questionExperimentId)) {
            throw new BadRequestException("Question does not belong to the enrollment's experiment");
        }

        LocalDateTime now = LocalDateTime.now();
        var phase = question.getPhase();
        if (phase.getStartDate() != null && phase.getStartDate().isAfter(now)) {
            throw new BadRequestException("This phase has not started yet");
        }
        if (phase.getEndDate() != null && phase.getEndDate().isBefore(now)) {
            throw new BadRequestException("This phase has already ended");
        }

        // En Between-Subjects la fase debe ser común (sin grupo) o del grupo del participante
        if (enrollment.getExperiment().getDesignType() == DesignType.BETWEEN_SUBJECTS) {
            if (phase.getGroup() != null) {
                Long phaseGroupId = phase.getGroup().getId();
                Long participantGroupId = enrollment.getGroup() != null ? enrollment.getGroup().getId() : null;
                if (!phaseGroupId.equals(participantGroupId)) {
                    throw new BadRequestException("This phase does not belong to your assigned group");
                }
            }
        }

        // En PRETEST_POSTTEST bloqueamos una fase hasta que la anterior esté completamente respondida
        if (enrollment.getExperiment().getDesignType() == DesignType.PRETEST_POSTTEST) {
            List<Phase> fases = phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(
                    enrollment.getExperiment().getId()
            );

            int indiceFaseActual = -1;
            for (int i = 0; i < fases.size(); i++) {
                if (fases.get(i).getId().equals(phase.getId())) {
                    indiceFaseActual = i;
                    break;
                }
            }

            // Si no es la primera fase, verificamos que la anterior esté completa
            if (indiceFaseActual > 0) {
                Phase faseAnterior = fases.get(indiceFaseActual - 1);
                long totalPreguntas = questionRepository.countByPhaseId(faseAnterior.getId());
                long respuestasDadas = responseRepository
                        .findByEnrollmentIdAndPhaseId(enrollmentId, faseAnterior.getId())
                        .size();

                if (respuestasDadas < totalPreguntas) {
                    throw new BadRequestException(
                            "Debes completar la fase '" + faseAnterior.getName()
                            + "' antes de acceder a esta fase."
                    );
                }
            }
        }

        // Si ya respondió antes, actualizamos; si no, creamos nueva respuesta
        Response response = responseRepository
                .findByEnrollmentIdAndQuestionId(enrollmentId, request.getQuestionId())
                .orElse(new Response(enrollment, question));

        validateAndSetResponseValue(response, question, request);

        Response savedResponse = responseRepository.save(response);

        // Para CROSS_SECTIONAL: cuando se responden todas las preguntas, completar la inscripción automáticamente
        if (enrollment.getExperiment().getDesignType() == DesignType.CROSS_SECTIONAL) {
            List<Phase> fases = phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(
                    enrollment.getExperiment().getId()
            );

            if (!fases.isEmpty()) {
                Phase fasePrincipal = fases.get(0);
                long totalPreguntas = questionRepository.countByPhaseId(fasePrincipal.getId());
                long respuestasActuales = responseRepository.findByEnrollmentId(enrollmentId).size();

                if (respuestasActuales >= totalPreguntas && enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                    enrollment.setStatus(EnrollmentStatus.COMPLETED);
                    enrollment.setCompletedAt(LocalDateTime.now());
                    enrollmentRepository.save(enrollment);
                }
            }
        }

        return convertToDTO(savedResponse);
    }

    private void validateAndSetResponseValue(Response response, Question question, SubmitResponseRequest request) {
        switch (question.getType()) {
            case TEXT:
                if (request.getTextValue() == null && question.getRequired()) {
                    throw new BadRequestException("Text value is required for this question");
                }
                response.setTextValue(request.getTextValue());
                break;

            case MULTIPLE_CHOICE:
                if (request.getTextValue() == null && question.getRequired()) {
                    throw new BadRequestException("An option must be selected for this question");
                }
                if (request.getTextValue() != null && question.getOptions() != null
                        && !question.getOptions().contains(request.getTextValue())) {
                    throw new BadRequestException(
                            "Invalid option '" + request.getTextValue() + "'. Valid options are: "
                            + String.join(", ", question.getOptions()));
                }
                response.setTextValue(request.getTextValue());
                break;

            case NUMBER:
            case SCALE:
                if (request.getNumericValue() == null && question.getRequired()) {
                    throw new BadRequestException("Numeric value is required for this question");
                }
                if (request.getNumericValue() != null) {
                    if (question.getMinValue() != null && request.getNumericValue() < question.getMinValue()) {
                        throw new BadRequestException("Value is below minimum: " + question.getMinValue());
                    }
                    if (question.getMaxValue() != null && request.getNumericValue() > question.getMaxValue()) {
                        throw new BadRequestException("Value is above maximum: " + question.getMaxValue());
                    }
                }
                response.setNumericValue(request.getNumericValue());
                break;

            case BOOLEAN:
                if (request.getBooleanValue() == null && question.getRequired()) {
                    throw new BadRequestException("Boolean value is required for this question");
                }
                response.setBooleanValue(request.getBooleanValue());
                break;
        }
    }

    public List<ResponseDTO> getResponsesByEnrollment(Long enrollmentId) {
        return responseRepository.findByEnrollmentId(enrollmentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResponseDTO> getResponsesByQuestion(Long questionId) {
        return responseRepository.findByQuestionId(questionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResponseDTO> getResponsesByEnrollmentAndPhase(Long enrollmentId, Long phaseId) {
        return responseRepository.findByEnrollmentIdAndPhaseId(enrollmentId, phaseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResponseDTO> getResponsesByExperiment(Long experimentId) {
        return responseRepository.findByExperimentId(experimentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteResponse(Long id) {
        if (!responseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Response not found");
        }
        responseRepository.deleteById(id);
    }

    public ResponseDTO convertToDTO(Response response) {
        return new ResponseDTO(
                response.getId(),
                response.getEnrollment().getId(),
                response.getQuestion().getId(),
                response.getQuestion().getText(),
                response.getTextValue(),
                response.getNumericValue(),
                response.getBooleanValue(),
                response.getSubmittedAt()
        );
    }
}
