package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.models.entities.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByTokenAndTokenType(String token, UserToken.TokenType tokenType);
    void deleteByUserAndTokenType(User user, UserToken.TokenType tokenType);
}
