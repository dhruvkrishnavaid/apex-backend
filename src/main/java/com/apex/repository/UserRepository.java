package com.apex.repository;

import com.apex.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  long countByRoleIdAndDeletedAtIsNull(UUID roleId);

  @Query(value = "SELECT u FROM User u JOIN FETCH u.role WHERE u.deletedAt IS NULL", countQuery = "SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
  Page<User> findAllActiveWithRole(Pageable pageable);

  Optional<User> findByEmailAndDeletedAtIsNull(String email);

  boolean existsByEmailAndDeletedAtIsNull(String email);

  Page<User> findAllByDeletedAtIsNull(Pageable pageable);

  @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<User> findByIdWithRole(@Param("id") UUID id);
}
