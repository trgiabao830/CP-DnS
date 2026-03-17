package com.tgb.cp_dns.repository.auth;

import com.tgb.cp_dns.entity.auth.PasswordResetToken;
import com.tgb.cp_dns.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    void deleteByUser(User user);
}
