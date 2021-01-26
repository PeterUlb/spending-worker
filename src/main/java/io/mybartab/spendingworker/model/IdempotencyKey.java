package io.mybartab.spendingworker.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true)
    private String idempotencyKey;

    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
