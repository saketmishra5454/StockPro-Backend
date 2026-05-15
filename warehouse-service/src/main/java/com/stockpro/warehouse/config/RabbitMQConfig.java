package com.stockpro.warehouse.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE              = "stockpro.exchange";
    public static final String MOVEMENT_QUEUE        = "stock.movement.queue";
    public static final String ALERT_QUEUE           = "stock.alert.queue";
    public static final String MOVEMENT_ROUTING_KEY  = "stock.movement.#";
    public static final String ALERT_ROUTING_KEY     = "stock.alert.#";

    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue stockMovementQueue() {
        return new Queue(MOVEMENT_QUEUE, true);
    }

    @Bean
    public Queue stockAlertQueue() {
        return new Queue(ALERT_QUEUE, true);
    }

    @Bean
    public Binding movementBinding() {
        return BindingBuilder
                .bind(stockMovementQueue())
                .to(stockproExchange())
                .with(MOVEMENT_ROUTING_KEY);
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder
                .bind(stockAlertQueue())
                .to(stockproExchange())
                .with(ALERT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
