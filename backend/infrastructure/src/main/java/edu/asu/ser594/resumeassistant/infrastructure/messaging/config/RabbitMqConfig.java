package edu.asu.ser594.resumeassistant.infrastructure.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_AI_DIRECT = "ai.direct.exchange";

    public static final String ROUTING_KEY_REQ_JOB_PARSE = "ai.req.job.parse";
    public static final String QUEUE_REQ_JOB_PARSE = "ai.queue.job.parse";
    public static final String ROUTING_KEY_RES_JOB_PARSE = "backend.res.job.parse";
    public static final String QUEUE_RES_JOB_PARSE = "backend.queue.job.parse";

    public static final String ROUTING_KEY_REQ_RESUME_PARSE = "ai.req.resume.parse";
    public static final String QUEUE_REQ_RESUME_PARSE = "ai.queue.resume.parse";
    public static final String ROUTING_KEY_RES_RESUME_PARSE = "backend.res.resume.parse";
    public static final String QUEUE_RES_RESUME_PARSE = "backend.queue.resume.parse";

    public static final String ROUTING_KEY_REQ_VECTOR_GEN = "ai.req.vector.gen";
    public static final String QUEUE_REQ_VECTOR_GEN = "ai.queue.vector.gen";
    public static final String ROUTING_KEY_RES_VECTOR_GEN = "backend.res.vector.gen";
    public static final String QUEUE_RES_VECTOR_GEN = "backend.queue.vector.gen";

    public static final String ROUTING_KEY_REQ_CONVERSATION = "ai.req.conversation";
    public static final String QUEUE_REQ_CONVERSATION = "ai.queue.conversation";
    public static final String ROUTING_KEY_RES_CONVERSATION = "backend.res.conversation";
    public static final String QUEUE_RES_CONVERSATION = "backend.queue.conversation";

    public static final String ROUTING_KEY_REQ_JOB_RANK = "ai.req.job.rank";
    public static final String QUEUE_REQ_JOB_RANK = "ai.queue.job.rank";
    public static final String ROUTING_KEY_RES_JOB_RANK = "backend.res.job.rank";
    public static final String QUEUE_RES_JOB_RANK = "backend.queue.job.rank";

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange aiDirectExchange() {
        return new DirectExchange(EXCHANGE_AI_DIRECT);
    }

    @Bean
    public Queue reqJobParseQueue() {
        return QueueBuilder.durable(QUEUE_REQ_JOB_PARSE).build();
    }

    @Bean
    public Binding reqJobParseBinding(Queue reqJobParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqJobParseQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_JOB_PARSE);
    }

    @Bean
    public Queue resJobParseQueue() {
        return QueueBuilder.durable(QUEUE_RES_JOB_PARSE).build();
    }

    @Bean
    public Binding resJobParseBinding(Queue resJobParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resJobParseQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_JOB_PARSE);
    }

    @Bean
    public Queue reqResumeParseQueue() {
        return QueueBuilder.durable(QUEUE_REQ_RESUME_PARSE).build();
    }

    @Bean
    public Binding reqResumeParseBinding(Queue reqResumeParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqResumeParseQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_RESUME_PARSE);
    }

    @Bean
    public Queue resResumeParseQueue() {
        return QueueBuilder.durable(QUEUE_RES_RESUME_PARSE).build();
    }

    @Bean
    public Binding resResumeParseBinding(Queue resResumeParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resResumeParseQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_RESUME_PARSE);
    }

    @Bean
    public Queue reqVectorGenQueue() {
        return QueueBuilder.durable(QUEUE_REQ_VECTOR_GEN).build();
    }

    @Bean
    public Binding reqVectorGenBinding(Queue reqVectorGenQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqVectorGenQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_VECTOR_GEN);
    }

    @Bean
    public Queue resVectorGenQueue() {
        return QueueBuilder.durable(QUEUE_RES_VECTOR_GEN).build();
    }

    @Bean
    public Binding resVectorGenBinding(Queue resVectorGenQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resVectorGenQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_VECTOR_GEN);
    }

    @Bean
    public Queue reqConversationQueue() {
        return QueueBuilder.durable(QUEUE_REQ_CONVERSATION).build();
    }

    @Bean
    public Binding reqConversationBinding(Queue reqConversationQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqConversationQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_CONVERSATION);
    }

    @Bean
    public Queue resConversationQueue() {
        return QueueBuilder.durable(QUEUE_RES_CONVERSATION).build();
    }

    @Bean
    public Binding resConversationBinding(Queue resConversationQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resConversationQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_CONVERSATION);
    }

    @Bean
    public Queue reqJobRankQueue() {
        return QueueBuilder.durable(QUEUE_REQ_JOB_RANK).build();
    }

    @Bean
    public Binding reqJobRankBinding(Queue reqJobRankQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqJobRankQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_JOB_RANK);
    }

    @Bean
    public Queue resJobRankQueue() {
        return QueueBuilder.durable(QUEUE_RES_JOB_RANK).build();
    }

    @Bean
    public Binding resJobRankBinding(Queue resJobRankQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resJobRankQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_JOB_RANK);
    }
}
