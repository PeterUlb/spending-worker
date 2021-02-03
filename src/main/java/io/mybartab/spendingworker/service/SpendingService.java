package io.mybartab.spendingworker.service;

import io.mybartab.spendingworker.dto.SpendingMessageDto;
import io.mybartab.spendingworker.dto.SpendingResponseDto;

import java.math.BigDecimal;
import java.util.Optional;

public interface SpendingService {
    SpendingResponseDto addSpending(SpendingMessageDto spendingMessageDto);

    Optional<BigDecimal> getSumForGroup(String groupId);
}
