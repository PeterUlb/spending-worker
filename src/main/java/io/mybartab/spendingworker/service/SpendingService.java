package io.mybartab.spendingworker.service;

import io.mybartab.spendingworker.dto.SpendingMessageDto;

public interface SpendingService {
    void addSpending(SpendingMessageDto spendingMessageDto);
}
