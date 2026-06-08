package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCleanupService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * Runs every hour to clean up guest accounts older than 24 hours 
     * that have no active orders.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupInactiveGuests() {
        log.info("Starting guest account cleanup task...");
        
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<User> inactiveGuests = userRepository.findInactiveGuests(threshold);
        
        if (!inactiveGuests.isEmpty()) {
            userRepository.deleteAll(inactiveGuests);
            log.info("Successfully cleaned up {} guest accounts older than 24 hours.", inactiveGuests.size());
        } else {
            log.info("No inactive guest accounts found for cleanup.");
        }
    }
}
