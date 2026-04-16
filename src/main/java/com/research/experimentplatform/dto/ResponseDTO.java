package com.research.experimentplatform.dto;

import java.time.LocalDateTime;

public class ResponseDTO {

    private Long id;
    private Long enrollmentId;
    private Long questionId;
    private String questionText;
    private String textValue;
    private Integer numericValue;
    private Boolean booleanValue;
    private LocalDateTime submittedAt;

    public ResponseDTO() {
    }

    public ResponseDTO(Long id, Long enrollmentId, Long questionId, String questionText,
                       String textValue, Integer numericValue, Boolean booleanValue,
                       LocalDateTime submittedAt) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.questionId = questionId;
        this.questionText = questionText;
        this.textValue = textValue;
        this.numericValue = numericValue;
        this.booleanValue = booleanValue;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public Integer getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Integer numericValue) {
        this.numericValue = numericValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
