package com.apex.repository;

import com.apex.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

  @Modifying
  @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
  void revokeAllByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
  void deleteAllExpiredOrRevoked(@Param("now") Instant now);
}
