package io.mybartab.spendingworker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mybartab.spendingworker.dto.IdempotencyDto;
import io.mybartab.spendingworker.dto.SpendingMessageDto;
import io.mybartab.spendingworker.dto.SpendingResponseDto;
import io.mybartab.spendingworker.model.Idempotency;
import io.mybartab.spendingworker.model.IdempotencyStatus;
import io.mybartab.spendingworker.model.Spending;
import io.mybartab.spendingworker.model.SpendingGroup;
import io.mybartab.spendingworker.repository.IdempotencyRepository;
import io.mybartab.spendingworker.repository.SpendingGroupRepository;
import io.mybartab.spendingworker.repository.SpendingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class SpendingServiceImpl implements SpendingService {
    private final SpendingGroupRepository spendingGroupRepository;
    private final SpendingRepository spendingRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public SpendingServiceImpl(SpendingGroupRepository spendingGroupRepository, SpendingRepository spendingRepository, IdempotencyRepository idempotencyRepository, PlatformTransactionManager platformTransactionManager, ObjectMapper objectMapper) {
        this.spendingGroupRepository = spendingGroupRepository;
        this.spendingRepository = spendingRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.objectMapper = objectMapper;
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
//    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SpendingResponseDto addSpending(SpendingMessageDto spendingMessageDto) {
        SpendingGroup spendingGroup;
        try {
            spendingGroup = spendingGroupRepository
                    .findByExternalId(spendingMessageDto.getSpendingGroupId())
                    .orElseGet(() -> spendingGroupRepository.save(SpendingGroup.builder()
                            .externalId(spendingMessageDto.getSpendingGroupId())
                            .description("Banana").build()));
        } catch (DataIntegrityViolationException e) {
            // Parallel Insert Violates Unique Key, get the entry
            // TODO: How to silence SqlExceptionHelper
            spendingGroup = spendingGroupRepository.findByExternalId(spendingMessageDto.getSpendingGroupId()).orElseThrow();
        }

        if (spendingGroup == null) {
            log.error("spendingGroup is null, but that should be impossible");
            throw new RuntimeException("❤"); // TODO: Real exception
        }

        IdempotencyDto idempotencyDto;
        try {
            // Needs transaction for LOB (TEXT)
            idempotencyDto = transactionTemplate.execute(transactionStatus -> idempotencyRepository.findByIdempotencyKey(spendingMessageDto.getIdempotencyKey(), IdempotencyDto.class)
                    .orElseGet(() -> {
                        Idempotency idempotency = idempotencyRepository.save(Idempotency.builder()
                                .idempotencyKey(spendingMessageDto.getIdempotencyKey())
                                .status(IdempotencyStatus.CREATED)
                                .build());

                        return new IdempotencyDto(idempotency.getIdempotencyKey(), idempotency.getStatus(), idempotency.getCode(), idempotency.getResponse());
                    }));
        } catch (DataIntegrityViolationException e) {
            // Parallel Insert Violates Unique Key, get the entry
            // Needs transaction for LOB (TEXT)
            idempotencyDto = transactionTemplate.execute(transactionStatus -> idempotencyRepository.findByIdempotencyKey(spendingMessageDto.getIdempotencyKey(), IdempotencyDto.class).orElseThrow());
        }

        if (idempotencyDto == null) {
            log.error("idempotencyDto is null, but that should be impossible");
            throw new RuntimeException("❤"); // TODO: Real exception
        }

        switch (idempotencyDto.getStatus()) {
            case ERROR_NOT_RETRYABLE, FINISHED -> {
                log.info("Returning cached response: " + idempotencyDto.getStatus() + " - " + idempotencyDto.getResponse());
                try {
                    return objectMapper.readValue(idempotencyDto.getResponse(), SpendingResponseDto.class);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize response");
                    throw new RuntimeException("❤"); // TODO: Real exception
                }
            }
        }

        // Now try to lock the row
        final SpendingGroup spendingGroup1 = spendingGroup;
        SpendingResponseDto spendingResponseDtoReturn = transactionTemplate.execute(transactionStatus -> {
            // PESSIMISTIC_WRITE, SHOULD block all others
            Idempotency idm = idempotencyRepository.findByIdempotencyKeyPW(spendingMessageDto.getIdempotencyKey()).orElseThrow();
            log.info("Status is: " + idm.getStatus());
            switch (idm.getStatus()) {
                case ERROR_NOT_RETRYABLE, FINISHED -> {
                    log.info("Returning response: " + idm.getStatus() + " - " + idm.getResponse());
                    return objectMapper.convertValue(idm.getResponse(), SpendingResponseDto.class);
                }
            }
            log.info("-----------PROCESSING----------");

            SpendingResponseDto spendingResponseDto = new SpendingResponseDto(HttpStatus.OK.value(),
                    spendingGroup1.getExternalId(), new BigDecimal(spendingMessageDto.getAmount()));

            Spending spending = Spending.builder()
                    .spendingGroupId(spendingGroup1.getId())
                    .amount(new BigDecimal(spendingMessageDto.getAmount()))
                    .build();
            spendingRepository.save(spending);
            idm.setStatus(IdempotencyStatus.FINISHED);
            idm.setCode(HttpStatus.OK.value());
            try {
                idm.setResponse(objectMapper.writeValueAsString(spendingResponseDto));
            } catch (JsonProcessingException e) {
                log.error("Could not serialize response");
                idm.setStatus(IdempotencyStatus.ERROR_NOT_RETRYABLE);
                idm.setResponse("{}"); // TODO
            }
            return spendingResponseDto;
        });

        if (spendingResponseDtoReturn == null) {
            log.error("spendingResponseDtoReturn is null, but that should be impossible");
            throw new RuntimeException("❤"); // TODO: Real exception
        }

        return spendingResponseDtoReturn;
    }

    @Override
    public Optional<BigDecimal> getSumForGroup(String groupId) {
        return spendingGroupRepository.findByExternalId(groupId).map(spendingGroup -> spendingRepository.getSum(spendingGroup.getId()));
    }
}
