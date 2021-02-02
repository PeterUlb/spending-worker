package io.mybartab.spendingworker.repository;

import io.mybartab.spendingworker.model.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<Idempotency, Long> {
    @Query("SELECT i FROM Idempotency i where i.idempotencyKey = :idempotencyKey")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Idempotency> findByIdempotencyKeyPW(String idempotencyKey);

    Optional<Idempotency> findByIdempotencyKey(String idempotencyKey);
}
