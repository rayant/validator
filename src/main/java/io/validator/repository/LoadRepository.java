package io.validator.repository;

import io.validator.entity.LoadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoadRepository extends JpaRepository<LoadEntity, String> {

    @Query(value = "SELECT coalesce(sum(l.load_amount),0) from load l where l.customer_id = :customerId and  l.daily_count_accepted is true and l.daily_limit_accepted is true and l.weekly_limit_accepted is true and timestamp >= :from and timestamp <= :to", nativeQuery = true)
    BigDecimal getValidLoadAmountBetweenDates(String customerId, LocalDateTime from, LocalDateTime to);
    @Query(value = "SELECT coalesce(count (*), 0)  from load l where l.customer_id = :customerId and  l.daily_count_accepted is true and l.daily_limit_accepted is true and l.weekly_limit_accepted is true and timestamp >= :from and timestamp <= :to", nativeQuery = true)
    int getValidLoadCountBetweenDates(String customerId, LocalDateTime from, LocalDateTime to);

    List<LoadEntity> findAllByCustomerIdOrderByTimeDesc(String customerId);

    Optional<LoadEntity> findByCustomerIdAndLoadId(String customerId, String loadId);
}
