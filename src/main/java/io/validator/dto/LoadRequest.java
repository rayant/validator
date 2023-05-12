package io.validator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoadRequest {

    @NotBlank(message = "id is mandatory")
    private String id;
    @NotBlank(message = "customer_id is mandatory")
    @JsonProperty("customer_id")
    private String customerId;
    @Pattern(regexp = "\\$\\d+\\.\\d{2}")
    @JsonProperty("load_amount")
    private String loadAmount;
    @NotBlank(message = "time is mandatory")
    private LocalDateTime time;
}
