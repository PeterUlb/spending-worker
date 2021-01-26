package io.mybartab.spendingworker.repository;

import io.mybartab.spendingworker.model.Spending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SpendingRepository extends JpaRepository<Spending, Long> {
    @Query("SELECT SUM(s.amount) FROM Spending s WHERE s.spendingGroupId = :spendingGroupId")
    BigDecimal getSum(long spendingGroupId);
}
