package io.mybartab.spendingworker.service;

import io.mybartab.spendingworker.dto.SpendingMessageDto;
import io.mybartab.spendingworker.model.IdempotencyKey;
import io.mybartab.spendingworker.model.Spending;
import io.mybartab.spendingworker.model.SpendingGroup;
import io.mybartab.spendingworker.repository.IdempotencyKeyRepository;
import io.mybartab.spendingworker.repository.SpendingGroupRepository;
import io.mybartab.spendingworker.repository.SpendingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class SpendingServiceImpl implements SpendingService {
    private final SpendingGroupRepository spendingGroupRepository;
    private final SpendingRepository spendingRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public SpendingServiceImpl(SpendingGroupRepository spendingGroupRepository, SpendingRepository spendingRepository, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.spendingGroupRepository = spendingGroupRepository;
        this.spendingRepository = spendingRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addSpending(SpendingMessageDto spendingMessageDto) {
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .idempotencyKey(spendingMessageDto.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());


        SpendingGroup spendingGroup = spendingGroupRepository.findByExternalId(spendingMessageDto.getSpendingGroupId()).orElseGet(() -> {
            log.warn("Spending group " + spendingMessageDto.getSpendingGroupId() + " does not exists, creating...");
            SpendingGroup sG = SpendingGroup.builder()
                    .externalId(spendingMessageDto.getSpendingGroupId()).build();
            spendingGroupRepository.save(sG);
            return sG;
        });

        Spending spending = Spending.builder()
                .spendingGroupId(spendingGroup.getId())
                .amount(new BigDecimal(spendingMessageDto.getAmount()))
                .build();
        spendingRepository.save(spending);
    }
}
