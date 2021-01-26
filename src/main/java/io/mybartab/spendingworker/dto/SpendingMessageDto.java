package io.mybartab.spendingworker.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class SpendingMessageDto {
    private String spendingGroupId;
    private String idempotencyKey;
    private String amount; //TODO: Cents instead
    private String currencyId;
}
