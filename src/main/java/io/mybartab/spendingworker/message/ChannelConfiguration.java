package io.mybartab.spendingworker.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import io.mybartab.spendingworker.config.GcpProperties;
import io.mybartab.spendingworker.dto.SpendingMessageDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class ChannelConfiguration {
    private final GcpProperties gcpProperties;

    public ChannelConfiguration(GcpProperties gcpProperties) {
        this.gcpProperties = gcpProperties;
    }

    @Bean
    public PubSubInboundChannelAdapter pubSubInboundChannelAdapter(@Qualifier("add-spending-v1-in") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, gcpProperties.getSubscriptionName());
        adapter.setOutputChannel(inputChannel);
        adapter.setPayloadType(SpendingMessageDto.class);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean(name = "add-spending-v1-in")
    public MessageChannel pubsubInputChannel() {
        return new DirectChannel();
    }


    @Bean
    public PubSubMessageConverter pubSubMessageConverter() {
        return new JacksonPubSubMessageConverter(new ObjectMapper());
    }
}
