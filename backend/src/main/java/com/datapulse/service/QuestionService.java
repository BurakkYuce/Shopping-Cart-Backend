package com.datapulse.service;

import com.datapulse.dto.request.AnswerQuestionRequest;
import com.datapulse.dto.request.CreateQuestionRequest;
import com.datapulse.dto.response.QuestionResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.Product;
import com.datapulse.model.Question;
import com.datapulse.model.RoleType;
import com.datapulse.model.Store;
import com.datapulse.repository.ProductRepository;
import com.datapulse.repository.QuestionRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public List<QuestionResponse> getByProductId(String productId) {
        return questionRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(QuestionResponse::from).toList();
    }

    public QuestionResponse ask(CreateQuestionRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", req.getProductId()));

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Question q = new Question();
        q.setId(id);
        q.setProductId(req.getProductId());
        q.setUserId(currentUser.getId());
        q.setQuestion(req.getQuestion());
        q.setCreatedAt(LocalDateTime.now());

        questionRepository.save(q);
        return QuestionResponse.from(questionRepository.findById(id).orElse(q));
    }

    public QuestionResponse answer(String questionId, AnswerQuestionRequest req, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question", questionId));

        Product product = productRepository.findById(question.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product", question.getProductId()));

        if (role == RoleType.CORPORATE) {
            Store store = storeRepository.findById(product.getStoreId())
                    .orElseThrow(() -> new EntityNotFoundException("Store", product.getStoreId()));
            if (!store.getOwnerId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Access denied: you do not own this product's store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: CORPORATE or ADMIN role required");
        }

        question.setAnswer(req.getAnswer());
        question.setAnsweredByUserId(currentUser.getId());
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);
        return QuestionResponse.from(questionRepository.findById(questionId).orElse(question));
    }

    public void delete(String questionId, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question", questionId));

        boolean isAdmin = role == RoleType.ADMIN;
        boolean isOwner = question.getUserId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedAccessException("Access denied: you can only delete your own questions");
        }

        questionRepository.delete(question);
    }
}
