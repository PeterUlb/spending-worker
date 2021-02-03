package io.mybartab.spendingworker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SpendingResponseDto {
    private int code;
    private String groupId;
    private BigDecimal amount;
}
