package com.datapulse.repository;

import com.datapulse.model.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    Page<ChatConversation> findByUserId(String userId, Pageable pageable);
    Optional<ChatConversation> findByIdAndUserId(String id, String userId);
}
