package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {
    List<AIMessage> findAllBySessionIdOrderByTimestampAsc(Long sessionId);
}
