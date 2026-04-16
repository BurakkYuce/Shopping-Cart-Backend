package com.datapulse.repository;

import com.datapulse.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {
    List<Question> findByProductIdOrderByCreatedAtDesc(String productId);
}
