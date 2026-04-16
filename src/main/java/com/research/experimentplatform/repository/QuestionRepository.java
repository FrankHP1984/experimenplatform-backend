package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByPhaseId(Long phaseId);
    
    List<Question> findByPhaseIdOrderByQuestionOrderAsc(Long phaseId);
    
    long countByPhaseId(Long phaseId);
}
