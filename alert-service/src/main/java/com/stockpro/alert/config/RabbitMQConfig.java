package com.stockpro.alert.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "stockpro.exchange";
    public static final String ALERT_QUEUE = "stock.alert.queue";
    public static final String ALERT_ROUTING_KEY = "stock.alert.#";

    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue stockAlertQueue() {
        return new Queue(ALERT_QUEUE, true);
    }

    @Bean
    public Binding stockAlertBinding() {
        return BindingBuilder
                .bind(stockAlertQueue())
                .to(stockproExchange())
                .with(ALERT_ROUTING_KEY);
    }
}
