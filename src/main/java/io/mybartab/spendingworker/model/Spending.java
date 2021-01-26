package io.mybartab.spendingworker.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Spending {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Long spendingGroupId;

    @Column
    private BigDecimal amount;
}
