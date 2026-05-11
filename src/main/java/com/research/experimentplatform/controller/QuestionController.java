package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.CreateQuestionRequest;
import com.research.experimentplatform.dto.UpdateQuestionRequest;
import com.research.experimentplatform.dto.QuestionDTO;
import com.research.experimentplatform.dto.ResponseDTO;
import com.research.experimentplatform.dto.SubmitResponseRequest;
import com.research.experimentplatform.service.QuestionService;
import com.research.experimentplatform.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final ResponseService responseService;

    public QuestionController(QuestionService questionService, ResponseService responseService) {
        this.questionService = questionService;
        this.responseService = responseService;
    }

    @PostMapping("/phases/{phaseId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<QuestionDTO> createQuestion(
            @PathVariable Long phaseId,
            @Valid @RequestBody CreateQuestionRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        QuestionDTO question = questionService.createQuestion(phaseId, request, supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(question);
    }

    @GetMapping("/phases/{phaseId}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByPhase(@PathVariable Long phaseId) {
        List<QuestionDTO> questions = questionService.getQuestionsByPhase(phaseId);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestion(@PathVariable Long id) {
        QuestionDTO question = questionService.getQuestion(id);
        return ResponseEntity.ok(question);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<QuestionDTO> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        QuestionDTO question = questionService.updateQuestion(id, request, supabaseId);
        return ResponseEntity.ok(question);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long id,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        questionService.deleteQuestion(id, supabaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/enrollments/{enrollmentId}/responses")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<ResponseDTO> submitResponse(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody SubmitResponseRequest request) {
        ResponseDTO response = responseService.submitResponse(enrollmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/enrollments/{enrollmentId}/responses")
    public ResponseEntity<List<ResponseDTO>> getResponsesByEnrollment(@PathVariable Long enrollmentId) {
        List<ResponseDTO> responses = responseService.getResponsesByEnrollment(enrollmentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{questionId}/responses")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<List<ResponseDTO>> getResponsesByQuestion(@PathVariable Long questionId) {
        List<ResponseDTO> responses = responseService.getResponsesByQuestion(questionId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/enrollments/{enrollmentId}/phases/{phaseId}/responses")
    public ResponseEntity<List<ResponseDTO>> getResponsesByEnrollmentAndPhase(
            @PathVariable Long enrollmentId,
            @PathVariable Long phaseId) {
        List<ResponseDTO> responses = responseService.getResponsesByEnrollmentAndPhase(enrollmentId, phaseId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/responses/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Void> deleteResponse(@PathVariable Long id) {
        responseService.deleteResponse(id);
        return ResponseEntity.noContent().build();
    }
}
