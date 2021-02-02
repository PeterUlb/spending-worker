package io.mybartab.spendingworker.service;

import io.mybartab.spendingworker.dto.SpendingMessageDto;
import io.mybartab.spendingworker.model.Idempotency;
import io.mybartab.spendingworker.model.IdempotencyStatus;
import io.mybartab.spendingworker.model.Spending;
import io.mybartab.spendingworker.model.SpendingGroup;
import io.mybartab.spendingworker.repository.IdempotencyRepository;
import io.mybartab.spendingworker.repository.SpendingGroupRepository;
import io.mybartab.spendingworker.repository.SpendingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Service
@Slf4j
public class SpendingServiceImpl implements SpendingService {
    private final SpendingGroupRepository spendingGroupRepository;
    private final SpendingRepository spendingRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TransactionTemplate transactionTemplate;

    public SpendingServiceImpl(SpendingGroupRepository spendingGroupRepository, SpendingRepository spendingRepository, IdempotencyRepository idempotencyRepository, PlatformTransactionManager platformTransactionManager) {
        this.spendingGroupRepository = spendingGroupRepository;
        this.spendingRepository = spendingRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
//    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addSpending(SpendingMessageDto spendingMessageDto) {
        SpendingGroup spendingGroup;
        try {
            spendingGroup = spendingGroupRepository
                    .findByExternalId(spendingMessageDto.getSpendingGroupId())
                    .orElseGet(() -> spendingGroupRepository.save(SpendingGroup.builder()
                            .externalId(spendingMessageDto.getSpendingGroupId())
                            .description("Banana").build()));
        } catch (DataIntegrityViolationException e) {
            // Parallel Insert Violates Unique Key, get the entry
            spendingGroup = spendingGroupRepository.findByExternalId(spendingMessageDto.getSpendingGroupId()).orElseThrow();
        }

        if (spendingGroup == null) {
            throw new RuntimeException("❤"); // TODO: Real exception
        }

        Idempotency idempotency;
        try {
            // Needs transaction for LOB (TEXT)
            idempotency = transactionTemplate.execute(transactionStatus -> idempotencyRepository.findByIdempotencyKey(spendingMessageDto.getIdempotencyKey())
                    .orElseGet(() -> idempotencyRepository.save(Idempotency.builder()
                            .idempotencyKey(spendingMessageDto.getIdempotencyKey())
                            .status(IdempotencyStatus.CREATED)
                            .build())));
        } catch (DataIntegrityViolationException e) {
            // Parallel Insert Violates Unique Key, get the entry
            idempotency = idempotencyRepository.findByIdempotencyKey(spendingMessageDto.getIdempotencyKey()).orElseThrow();
        }

        if (idempotency == null) {
            throw new RuntimeException("❤"); // TODO: Real exception
        }

        switch (idempotency.getStatus()) {
            case ERROR_NOT_RETRYABLE, FINISHED -> {
                log.info("Returning response: " + idempotency.getStatus() + " - " + idempotency.getResponse());
                return;
            }
        }

        // Now try to lock the row
        final SpendingGroup spendingGroup1 = spendingGroup;
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            // PESSIMISTIC_WRITE, SHOULD block all others
            Idempotency idm = idempotencyRepository.findByIdempotencyKeyPW(spendingMessageDto.getIdempotencyKey()).orElseThrow();
            switch (idm.getStatus()) {
                case ERROR_NOT_RETRYABLE, FINISHED -> {
                    log.info("Returning response: " + idm.getStatus() + " - " + idm.getResponse());
                    return;
                }
            }
            log.warn("-----------PROCESSING----------");

            Spending spending = Spending.builder()
                    .spendingGroupId(spendingGroup1.getId())
                    .amount(new BigDecimal(spendingMessageDto.getAmount()))
                    .build();
            spendingRepository.save(spending);
            idm.setStatus(IdempotencyStatus.FINISHED);
            idm.setCode(200);
            idm.setResponse("Ok");
        });
    }
}
