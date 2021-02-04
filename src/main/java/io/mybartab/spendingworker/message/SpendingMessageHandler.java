package io.mybartab.spendingworker.message;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import io.mybartab.spendingworker.dto.SpendingMessageDto;
import io.mybartab.spendingworker.service.SpendingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SpendingMessageHandler {
    private final SpendingService spendingService;

    public SpendingMessageHandler(SpendingService spendingService) {
        this.spendingService = spendingService;
    }

    @ServiceActivator(inputChannel = "add-spending-v1-in")
    public void handleMessage(SpendingMessageDto spendingMessageDto, @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) throws MessagingException {
        log.info("Message arrived! Payload: " + spendingMessageDto);
        spendingService.addSpending(spendingMessageDto);
        log.debug(spendingMessageDto.toString());
        message.ack();

//        try {
//            spendingService.addSpending(spendingMessageDto);
//            message.ack();
//        } catch (CannotAcquireLockException e) {
//            log.error("Message cannot acquire lock. Message must be send again");
//            message.nack();
//        } catch (DataIntegrityViolationException e) {
//            if (e.getCause() != null && e.getCause().getCause() != null &&
//                    e.getCause().getCause().getMessage().contains("idempotency_key")) {
//                log.info("Unique key already exists");
//                message.ack();
//                return;
//            }
//
//            log.error("Uff " + e.getClass().getName() + ": " + e.getMessage());
//            message.nack();
//        }
    }
}
