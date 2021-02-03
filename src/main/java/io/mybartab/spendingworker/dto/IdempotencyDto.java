package io.mybartab.spendingworker.dto;

import io.mybartab.spendingworker.model.IdempotencyStatus;
import lombok.Value;

@Value
public class IdempotencyDto {
    String idempotencyKey;
    IdempotencyStatus status;
    Integer code;
    String response;
}
