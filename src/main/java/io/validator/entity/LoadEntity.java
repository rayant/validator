package io.validator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.validator.dto.LoadRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "load")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LoadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "load_id")
    private String loadId;
    @Column(name = "customer_id")
    private String customerId;
    @Column(name = "load_amount")
    private BigDecimal loadAmount;
    @Column(name = "timestamp")
    private LocalDateTime time;
    @Column(name = "daily_count_accepted")
    private boolean dailyCountAccepted;
    @Column(name = "daily_limit_accepted")
    private boolean dailyLimitAccepted;
    @Column(name = "weekly_limit_accepted")
    private boolean weeklyLimitAccepted;

    public LoadEntity (LoadRequest request) {
        this.customerId = request.getCustomerId();
        this.time = request.getTime();
        this.loadAmount = new BigDecimal(request.getLoadAmount().replace("$",""));
        this.loadId = request.getId();
    }
}
