package com.notifyengine.repository;

import com.notifyengine.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @Query("SELECT k FROM ApiKey k JOIN FETCH k.tenant WHERE k.keyHash = :keyHash")
    Optional<ApiKey> findByKeyHash(@Param("keyHash") String keyHash);
}
