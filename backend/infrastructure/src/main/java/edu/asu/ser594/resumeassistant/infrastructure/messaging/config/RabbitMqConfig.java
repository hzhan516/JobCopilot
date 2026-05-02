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

/**
 * RabbitMQ 配置类 / RabbitMQ configuration
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_AI_DIRECT = "ai.direct.exchange";

    // 死信交换机与队列 / Dead letter exchange and queue
    public static final String EXCHANGE_DLX = "ai.dlx.exchange";
    public static final String QUEUE_DLQ = "ai.dlq.queue";
    public static final String ROUTING_KEY_DLQ = "dlq.routing.key";

    // 职位解析队列与路由键 / Job parse queue and routing keys
    public static final String ROUTING_KEY_REQ_JOB_PARSE = "ai.req.job.parse";
    public static final String QUEUE_REQ_JOB_PARSE = "ai.queue.job.parse";
    public static final String ROUTING_KEY_RES_JOB_PARSE = "backend.res.job.parse";
    public static final String QUEUE_RES_JOB_PARSE = "backend.queue.job.parse";

    // 简历解析队列与路由键 / Resume parse queue and routing keys
    public static final String ROUTING_KEY_REQ_RESUME_PARSE = "ai.req.resume.parse";
    public static final String QUEUE_REQ_RESUME_PARSE = "ai.queue.resume.parse";
    public static final String ROUTING_KEY_RES_RESUME_PARSE = "backend.res.resume.parse";
    public static final String QUEUE_RES_RESUME_PARSE = "backend.queue.resume.parse";

    // 向量生成队列与路由键 / Vector generation queue and routing keys
    public static final String ROUTING_KEY_REQ_VECTOR_GEN = "ai.req.vector.gen";
    public static final String QUEUE_REQ_VECTOR_GEN = "ai.queue.vector.gen";
    public static final String ROUTING_KEY_RES_VECTOR_GEN = "backend.res.vector.gen";
    public static final String QUEUE_RES_VECTOR_GEN = "backend.queue.vector.gen";

    // 对话队列与路由键 / Conversation queue and routing keys
    public static final String ROUTING_KEY_REQ_CONVERSATION = "ai.req.conversation";
    public static final String QUEUE_REQ_CONVERSATION = "ai.queue.conversation";
    public static final String ROUTING_KEY_RES_CONVERSATION = "backend.res.conversation";
    public static final String QUEUE_RES_CONVERSATION = "backend.queue.conversation";

    // 职位排名队列与路由键 / Job rank queue and routing keys
    public static final String ROUTING_KEY_REQ_JOB_RANK = "ai.req.job.rank";
    public static final String QUEUE_REQ_JOB_RANK = "ai.queue.job.rank";
    public static final String ROUTING_KEY_RES_JOB_RANK = "backend.res.job.rank";
    public static final String QUEUE_RES_JOB_RANK = "backend.queue.job.rank";

    /**
     * Jackson JSON 消息转换器 / Jackson JSON message converter
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate 配置 / RabbitTemplate configuration
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * AI 直连交换机 / AI direct exchange
     */
    @Bean
    public DirectExchange aiDirectExchange() {
        return new DirectExchange(EXCHANGE_AI_DIRECT);
    }

    /**
     * 死信交换机 / Dead letter exchange
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(EXCHANGE_DLX);
    }

    /**
     * 死信队列 / Dead letter queue
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    /**
     * 死信队列绑定 / Dead letter queue binding
     */
    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlqQueue).to(dlxExchange).with(ROUTING_KEY_DLQ);
    }

    // ========== 职位解析队列绑定 / Job parse queue bindings ==========

    @Bean
    public Queue reqJobParseQueue() {
        return QueueBuilder.durable(QUEUE_REQ_JOB_PARSE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding reqJobParseBinding(Queue reqJobParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqJobParseQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_JOB_PARSE);
    }

    @Bean
    public Queue resJobParseQueue() {
        return QueueBuilder.durable(QUEUE_RES_JOB_PARSE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding resJobParseBinding(Queue resJobParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resJobParseQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_JOB_PARSE);
    }

    // ========== 简历解析队列绑定 / Resume parse queue bindings ==========

    @Bean
    public Queue reqResumeParseQueue() {
        return QueueBuilder.durable(QUEUE_REQ_RESUME_PARSE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding reqResumeParseBinding(Queue reqResumeParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqResumeParseQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_RESUME_PARSE);
    }

    @Bean
    public Queue resResumeParseQueue() {
        return QueueBuilder.durable(QUEUE_RES_RESUME_PARSE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding resResumeParseBinding(Queue resResumeParseQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resResumeParseQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_RESUME_PARSE);
    }

    // ========== 向量生成队列绑定 / Vector generation queue bindings ==========

    @Bean
    public Queue reqVectorGenQueue() {
        return QueueBuilder.durable(QUEUE_REQ_VECTOR_GEN)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding reqVectorGenBinding(Queue reqVectorGenQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqVectorGenQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_VECTOR_GEN);
    }

    @Bean
    public Queue resVectorGenQueue() {
        return QueueBuilder.durable(QUEUE_RES_VECTOR_GEN)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding resVectorGenBinding(Queue resVectorGenQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resVectorGenQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_VECTOR_GEN);
    }

    // ========== 对话队列绑定 / Conversation queue bindings ==========

    @Bean
    public Queue reqConversationQueue() {
        return QueueBuilder.durable(QUEUE_REQ_CONVERSATION)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding reqConversationBinding(Queue reqConversationQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqConversationQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_CONVERSATION);
    }

    @Bean
    public Queue resConversationQueue() {
        return QueueBuilder.durable(QUEUE_RES_CONVERSATION)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding resConversationBinding(Queue resConversationQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resConversationQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_CONVERSATION);
    }

    // ========== 职位排名队列绑定 / Job rank queue bindings ==========

    @Bean
    public Queue reqJobRankQueue() {
        return QueueBuilder.durable(QUEUE_REQ_JOB_RANK)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding reqJobRankBinding(Queue reqJobRankQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(reqJobRankQueue).to(aiDirectExchange).with(ROUTING_KEY_REQ_JOB_RANK);
    }

    @Bean
    public Queue resJobRankQueue() {
        return QueueBuilder.durable(QUEUE_RES_JOB_RANK)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding resJobRankBinding(Queue resJobRankQueue, DirectExchange aiDirectExchange) {
        return BindingBuilder.bind(resJobRankQueue).to(aiDirectExchange).with(ROUTING_KEY_RES_JOB_RANK);
    }
}
