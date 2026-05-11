package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.CreateQuestionRequest;
import com.research.experimentplatform.dto.UpdateQuestionRequest;
import com.research.experimentplatform.dto.QuestionDTO;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.model.Phase;
import com.research.experimentplatform.model.Question;
import com.research.experimentplatform.model.QuestionType;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.PhaseRepository;
import com.research.experimentplatform.repository.QuestionRepository;
import com.research.experimentplatform.security.OwnershipChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final PhaseRepository phaseRepository;
    private final OwnershipChecker ownershipChecker;

    public QuestionService(QuestionRepository questionRepository, PhaseRepository phaseRepository,
                           OwnershipChecker ownershipChecker) {
        this.questionRepository = questionRepository;
        this.phaseRepository = phaseRepository;
        this.ownershipChecker = ownershipChecker;
    }

    @Transactional
    public QuestionDTO createQuestion(Long phaseId, CreateQuestionRequest request, String supabaseId) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));

        if (!ownershipChecker.canModify(phase.getExperiment(), supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }

        validateQuestionTypeConstraints(request);

        Question question = new Question();
        question.setText(request.getText());
        question.setType(request.getType());
        question.setOptions(request.getOptions());
        question.setMinValue(request.getMinValue());
        question.setMaxValue(request.getMaxValue());
        question.setRequired(request.getRequired() != null ? request.getRequired() : false);
        question.setPhase(phase);
        question.setQuestionOrder(request.getQuestionOrder());

        Question savedQuestion = questionRepository.save(question);
        return convertToDTO(savedQuestion);
    }

    public List<QuestionDTO> getQuestionsByPhase(Long phaseId) {
        return questionRepository.findByPhaseIdOrderByQuestionOrderAsc(phaseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public QuestionDTO getQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        return convertToDTO(question);
    }

    @Transactional
    public QuestionDTO updateQuestion(Long id, UpdateQuestionRequest request, String supabaseId) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!ownershipChecker.canModify(question.getPhase().getExperiment(), supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }

        if (request.getText() != null) question.setText(request.getText());
        if (request.getType() != null) question.setType(request.getType());
        if (request.getOptions() != null) question.setOptions(request.getOptions());
        if (request.getMinValue() != null) question.setMinValue(request.getMinValue());
        if (request.getMaxValue() != null) question.setMaxValue(request.getMaxValue());
        if (request.getRequired() != null) question.setRequired(request.getRequired());
        if (request.getQuestionOrder() != null) question.setQuestionOrder(request.getQuestionOrder());

        return convertToDTO(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long id, String supabaseId) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        if (!ownershipChecker.canModify(question.getPhase().getExperiment(), supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }
        questionRepository.delete(question);
    }

    private void validateQuestionTypeConstraints(CreateQuestionRequest request) {
        if (request.getType() == QuestionType.MULTIPLE_CHOICE) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new BadRequestException("MULTIPLE_CHOICE questions must have at least 2 options");
            }
        }
        if (request.getType() == QuestionType.SCALE) {
            if (request.getMinValue() == null || request.getMaxValue() == null) {
                throw new BadRequestException("SCALE questions must define both minValue and maxValue");
            }
        }
    }

    public long countQuestionsByPhase(Long phaseId) {
        return questionRepository.countByPhaseId(phaseId);
    }

    public QuestionDTO convertToDTO(Question question) {
        return new QuestionDTO(
                question.getId(),
                question.getText(),
                question.getType(),
                question.getOptions(),
                question.getMinValue(),
                question.getMaxValue(),
                question.getRequired(),
                question.getPhase().getId(),
                question.getQuestionOrder()
        );
    }
}
