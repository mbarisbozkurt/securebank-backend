package com.securebank.service;

import com.securebank.event.TransferCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransferEventPublisherTests {

    @Test
    void publishesTransferCompletedEventToConfiguredExchangeAndRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        TransferEventPublisher publisher = new TransferEventPublisher(
                rabbitTemplate,
                "securebank.events",
                "transfer.completed"
        );
        TransferCompletedEvent event = new TransferCompletedEvent(
                42L,
                10L,
                11L,
                "sender@example.com",
                "recipient@example.com",
                new BigDecimal("250.00"),
                "TRY",
                "Rent payment",
                Instant.parse("2026-05-17T01:35:00Z")
        );

        publisher.publishTransferCompleted(event);

        verify(rabbitTemplate).convertAndSend(
                "securebank.events",
                "transfer.completed",
                event
        );
    }
}
