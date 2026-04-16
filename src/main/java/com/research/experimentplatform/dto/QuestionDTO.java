package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.QuestionType;
import java.util.List;

public class QuestionDTO {

    private Long id;
    private String text;
    private QuestionType type;
    private List<String> options;
    private Integer minValue;
    private Integer maxValue;
    private Boolean required;
    private Long phaseId;
    private Integer questionOrder;

    public QuestionDTO() {
    }

    public QuestionDTO(Long id, String text, QuestionType type, List<String> options,
                       Integer minValue, Integer maxValue, Boolean required,
                       Long phaseId, Integer questionOrder) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.options = options;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.required = required;
        this.phaseId = phaseId;
        this.questionOrder = questionOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Long getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(Long phaseId) {
        this.phaseId = phaseId;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }
}
