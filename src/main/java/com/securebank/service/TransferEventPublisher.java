package com.securebank.service;

import com.securebank.event.TransferCompletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransferEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String eventsExchange;
    private final String transferCompletedRoutingKey;

    public TransferEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${securebank.rabbitmq.exchange}") String eventsExchange,
            @Value("${securebank.rabbitmq.transfer-completed-routing-key}") String transferCompletedRoutingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventsExchange = eventsExchange;
        this.transferCompletedRoutingKey = transferCompletedRoutingKey;
    }

    public void publishTransferCompleted(TransferCompletedEvent event) {
        rabbitTemplate.convertAndSend(eventsExchange, transferCompletedRoutingKey, event);
    }
}
