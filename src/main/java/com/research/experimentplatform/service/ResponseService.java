package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.ResponseDTO;
import com.research.experimentplatform.dto.SubmitResponseRequest;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Question;
import com.research.experimentplatform.model.Response;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.QuestionRepository;
import com.research.experimentplatform.repository.ResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuestionRepository questionRepository;

    public ResponseService(ResponseRepository responseRepository,
                          EnrollmentRepository enrollmentRepository,
                          QuestionRepository questionRepository) {
        this.responseRepository = responseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public ResponseDTO submitResponse(Long enrollmentId, SubmitResponseRequest request) {
        // Primero buscamos la inscripción del participante
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        // Validamos que la inscripción esté activa
        // Si está COMPLETED, WITHDRAWN o PENDING no se pueden enviar respuestas
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Enrollment is not active");
        }

        // Buscamos la pregunta a la que se está respondiendo
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        // Validación importante: la pregunta tiene que pertenecer al mismo experimento
        // que la inscripción del participante, si no sería un error
        // Navegamos por las relaciones: Question -> Phase -> Experiment
        Long enrollmentExperimentId = enrollment.getExperiment().getId();
        Long questionExperimentId = question.getPhase().getExperiment().getId();
        if (!enrollmentExperimentId.equals(questionExperimentId)) {
            throw new BadRequestException("Question does not belong to the enrollment's experiment");
        }

        // Buscamos si ya existe una respuesta para esta pregunta y esta inscripción
        Response response = responseRepository
                .findByEnrollmentIdAndQuestionId(enrollmentId, request.getQuestionId())
                .orElse(new Response(enrollment, question));

        // Validamos y seteamos el valor de la respuesta según el tipo de pregunta
        validateAndSetResponseValue(response, question, request);

        // Guardamos en la base de datos y devolvemos el DTO
        Response savedResponse = responseRepository.save(response);
        return convertToDTO(savedResponse);
    }

    private void validateAndSetResponseValue(Response response, Question question, SubmitResponseRequest request) {
        // Validamos y seteamos el valor de la respuesta según el tipo de pregunta
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
