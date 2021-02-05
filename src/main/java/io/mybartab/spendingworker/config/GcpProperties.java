package io.mybartab.spendingworker.config;

import io.mybartab.spendingworker.validator.PropertySet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app.gcp")
@Validated
@Getter
@Setter
@ToString
public class GcpProperties {
    @PropertySet
    private String subscriptionName;
}
