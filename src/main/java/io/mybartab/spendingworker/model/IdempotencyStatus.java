package io.mybartab.spendingworker.model;

public enum IdempotencyStatus {
    CREATED,
    ERROR_RETRYABLE,
    ERROR_NOT_RETRYABLE,
    FINISHED,
}
