package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.AIChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIChatSessionRepository extends JpaRepository<AIChatSession, Long> {
    List<AIChatSession> findAllByUserIdOrderByStartedAtDesc(Long userId);
}
