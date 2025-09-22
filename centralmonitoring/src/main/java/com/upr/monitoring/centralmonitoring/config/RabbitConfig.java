package com.upr.monitoring.centralmonitoring.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange metricsExchange() {
        return new TopicExchange("metrics.exchange");
    }

    @Bean
    public Queue metricsQueue() {
        return new Queue("metrics.queue", true);
    }

    @Bean
    public Binding binding(Queue metricsQueue, TopicExchange metricsExchange) {
        return BindingBuilder.bind(metricsQueue)
                             .to(metricsExchange)
                             .with("metrics.#");
    }
}
