package io.validator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.validator.dto.LoadRequest;
import io.validator.dto.LoadResponse;
import io.validator.entity.LoadEntity;
import io.validator.repository.LoadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final LoadRepository loadRepository;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    @Value("${validation.limit.daily}")
    private BigDecimal dailyLimit;
    @Value("${validation.limit.weekly}")
    private BigDecimal weeklyLimit;
    @Value("${validation.limit.daily.count}")
    private int dailyCountLimit;

    public LoadResponse validate(LoadRequest loadRequest) {
        String customerId = loadRequest.getCustomerId();
        RLock customerLock = redissonClient.getLock("customer_lock_" + customerId);
        try {
            if (customerLock.tryLock(10, TimeUnit.SECONDS)) {
                Optional<LoadEntity> loadEntity = loadRepository.findByCustomerIdAndLoadId(loadRequest.getCustomerId(), loadRequest.getId());

                return loadEntity.map(e -> LoadResponse.builder()
                                .id(loadRequest.getId())
                                .customerId(loadRequest.getCustomerId())
                                .accepted(e.isDailyCountAccepted() && e.isDailyLimitAccepted() && e.isWeeklyLimitAccepted())
                                .build())
                        .orElseGet(() -> {
                                    LoadEntity entity = new LoadEntity(loadRequest);
                                    LoadResponse loadResponse = validateEntity(entity);
                                    loadRepository.save(entity);
                                    return loadResponse;
                                }
                        );
            } else {
                return LoadResponse.builder()
                        .id(loadRequest.getId())
                        .customerId(loadRequest.getCustomerId())
                        .accepted(false)
                        .build();
            }
        } catch (InterruptedException e) {
            return LoadResponse.builder()
                    .id(loadRequest.getId())
                    .customerId(loadRequest.getCustomerId())
                    .accepted(false)
                    .build();
        } finally {
            customerLock.unlock();
        }
    }


    public String validateFile(MultipartFile file) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    LoadRequest loadRequest = objectMapper.readValue(line, LoadRequest.class);
                    LoadResponse response = validate(loadRequest);
                    stringBuilder.append(objectMapper.writeValueAsString(response));
                    stringBuilder.append("\n");
                } catch (JsonProcessingException e) {
                    log.error("error while parsing from file, loadRequest = {}", line);
                }
            }
        } catch (IOException e) {
            return "";
        }
        return stringBuilder.toString();
    }

    private LoadResponse validateEntity(LoadEntity entity) {
        boolean isDailyLimitValid = validateDailyLimit(entity);
        boolean isWeeklyLimitValid = validateWeeklyLimit(entity);
        boolean isDailyCountLimitValid = validateDailyCountLimit(entity);
        boolean isAcceptable = isDailyLimitValid && isWeeklyLimitValid && isDailyCountLimitValid;
        entity.setDailyCountAccepted(isDailyCountLimitValid);
        entity.setDailyLimitAccepted(isDailyLimitValid);
        entity.setWeeklyLimitAccepted(isWeeklyLimitValid);

        return LoadResponse.builder().id(entity.getLoadId()).customerId(entity.getCustomerId()).accepted(isAcceptable).build();
    }

    public boolean validateDailyLimit(LoadEntity loadRequest) {
        LocalDateTime dayStart = LocalDateTime.of(LocalDate.from(loadRequest.getTime()), LocalTime.MIDNIGHT);
        BigDecimal loadedDailyAmount = loadRepository.getValidLoadAmountBetweenDates(loadRequest.getCustomerId(), dayStart, loadRequest.getTime());
        boolean accepted = loadRequest.getLoadAmount().add(loadedDailyAmount).compareTo(dailyLimit) <= 0;
        if (accepted) {
            log.info("{} daily limit accepted for user {} and amount {}", loadRequest.getLoadId(), loadRequest.getCustomerId(), loadRequest.getLoadAmount());
        } else {
            log.warn("{} daily limit not accepted for user {} and amount {}, remaining limit is {}", loadRequest.getLoadId(), loadRequest.getCustomerId(), loadRequest.getLoadAmount(), dailyLimit.subtract(loadedDailyAmount));
        }

        return accepted;
    }

    public boolean validateWeeklyLimit(LoadEntity loadRequest) {
        LocalDateTime dayStartOfTheWeek = LocalDateTime.of(LocalDate.from(loadRequest.getTime()), LocalTime.MIDNIGHT).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        BigDecimal loadedWeeklyAmount = loadRepository.getValidLoadAmountBetweenDates(loadRequest.getCustomerId(), dayStartOfTheWeek, loadRequest.getTime());
        boolean accepted = loadRequest.getLoadAmount().add(loadedWeeklyAmount).compareTo(weeklyLimit) <= 0;
        if (accepted) {
            log.info("{} weekly limit accepted for user {} and amount {}", loadRequest.getLoadId(), loadRequest.getCustomerId(), loadRequest.getLoadAmount());
        } else {
            log.warn("{} weekly limit not accepted for user {} and amount {}, remaining limit is {}", loadRequest.getLoadId(), loadRequest.getCustomerId(), loadRequest.getLoadAmount(), weeklyLimit.subtract(loadedWeeklyAmount));
        }
        return accepted;
    }

    public boolean validateDailyCountLimit(LoadEntity loadRequest) {
        LocalDateTime dayStart = LocalDateTime.of(LocalDate.from(loadRequest.getTime()), LocalTime.MIDNIGHT);
        int loadedDailyAmount = loadRepository.getValidLoadCountBetweenDates(loadRequest.getCustomerId(), dayStart, loadRequest.getTime());
        boolean accepted = loadedDailyAmount < dailyCountLimit;
        if (accepted) {
            log.info("{} daily count limit accepted for user {}", loadRequest.getLoadId(), loadRequest.getCustomerId());
        } else {
            log.warn("{} daily count limit not accepted for user {}", loadRequest.getLoadId(), loadRequest.getCustomerId());
        }
        return accepted;
    }
}
