package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.QuestionType;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateQuestionRequest {

    @NotBlank(message = "Question text is required")
    @Size(max = 1000, message = "Question text must not exceed 1000 characters")
    private String text;

    @NotNull(message = "Question type is required")
    private QuestionType type;

    private List<String> options;

    private Integer minValue;

    private Integer maxValue;

    private Boolean required;

    @NotNull(message = "Question order is required")
    @Min(value = 1, message = "Question order must be at least 1")
    private Integer questionOrder;

    @AssertTrue(message = "minValue must be less than maxValue")
    public boolean isMinLessThanMax() {
        if (minValue == null || maxValue == null) return true;
        return minValue < maxValue;
    }

    public CreateQuestionRequest() {
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

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    private Boolean attentionCheck;
    private String expectedAnswer;

    public Boolean getAttentionCheck() { return attentionCheck; }
    public void setAttentionCheck(Boolean attentionCheck) { this.attentionCheck = attentionCheck; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
}
