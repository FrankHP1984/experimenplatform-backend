package com.research.experimentplatform.model;

import com.research.experimentplatform.config.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    private List<String> options;

    private Integer minValue;

    private Integer maxValue;

    @Column(nullable = false)
    private Boolean required;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(nullable = false)
    private Integer questionOrder;

    @Column(columnDefinition = "boolean default false")
    private boolean attentionCheck = false;

    @Column(length = 500)
    private String expectedAnswer;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Response> responses = new ArrayList<>();

    public Question(String text, QuestionType type, Phase phase, Integer questionOrder) {
        this.text = text;
        this.type = type;
        this.phase = phase;
        this.questionOrder = questionOrder;
        this.required = false;
    }
}
