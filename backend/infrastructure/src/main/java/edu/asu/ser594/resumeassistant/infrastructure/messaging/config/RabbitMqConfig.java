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

    public static final String EXCHANGE_AI_TOPIC = "ai.topic.exchange";

    // 职位解析 (Job Parsing)
    public static final String ROUTING_KEY_JOB_PROCESS_REQ = "ai.job.process.req";
    public static final String ROUTING_KEY_JOB_PROCESS_RES = "ai.job.process.res";
    public static final String QUEUE_JOB_PROCESS_REQ = "q.ai.job.process.req";
    public static final String QUEUE_JOB_PROCESS_RES = "q.ai.job.process.res";

    // 简历解析 (Resume Parsing)
    public static final String ROUTING_KEY_RESUME_PARSE_REQ = "ai.resume.parse.req";
    public static final String ROUTING_KEY_RESUME_PARSE_RES = "ai.resume.parse.res";
    public static final String QUEUE_RESUME_PARSE_REQ = "q.ai.resume.parse.req";
    public static final String QUEUE_RESUME_PARSE_RES = "q.ai.resume.parse.res";

    // 向量生成 (Vector Generation)
    public static final String ROUTING_KEY_VECTOR_GEN_REQ = "ai.vector.gen.req";
    public static final String ROUTING_KEY_VECTOR_GEN_RES = "ai.vector.gen.res";
    public static final String QUEUE_VECTOR_GEN_REQ = "q.ai.vector.gen.req";
    public static final String QUEUE_VECTOR_GEN_RES = "q.ai.vector.gen.res";

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
    public TopicExchange aiTopicExchange() {
        return new TopicExchange(EXCHANGE_AI_TOPIC);
    }

    // ---------------- Job Processing Queues & Bindings ----------------
    @Bean
    public Queue jobProcessReqQueue() {
        return QueueBuilder.durable(QUEUE_JOB_PROCESS_REQ).build();
    }

    @Bean
    public Binding jobProcessReqBinding(Queue jobProcessReqQueue, TopicExchange aiTopicExchange) {
        return BindingBuilder.bind(jobProcessReqQueue).to(aiTopicExchange).with(ROUTING_KEY_JOB_PROCESS_REQ);
    }

    @Bean
    public Queue jobProcessResQueue() {
        return QueueBuilder.durable(QUEUE_JOB_PROCESS_RES).build();
    }

    @Bean
    public Binding jobProcessResBinding(Queue jobProcessResQueue, TopicExchange aiTopicExchange) {
        return BindingBuilder.bind(jobProcessResQueue).to(aiTopicExchange).with(ROUTING_KEY_JOB_PROCESS_RES);
    }

    // ---------------- Resume Parsing Queues & Bindings ----------------
    @Bean
    public Queue resumeParseReqQueue() {
        return QueueBuilder.durable(QUEUE_RESUME_PARSE_REQ).build();
    }

    @Bean
    public Binding resumeParseReqBinding(Queue resumeParseReqQueue, TopicExchange aiTopicExchange) {
        return BindingBuilder.bind(resumeParseReqQueue).to(aiTopicExchange).with(ROUTING_KEY_RESUME_PARSE_REQ);
    }

    @Bean
    public Queue resumeParseResQueue() {
        return QueueBuilder.durable(QUEUE_RESUME_PARSE_RES).build();
    }

    @Bean
    public Binding resumeParseResBinding(Queue resumeParseResQueue, TopicExchange aiTopicExchange) {
        return BindingBuilder.bind(resumeParseResQueue).to(aiTopicExchange).with(ROUTING_KEY_RESUME_PARSE_RES);
    }

    // ---------------- Vector Gen Queues & Bindings ----------------
    @Bean
    public Queue vectorGenReqQueue() {
        return QueueBuilder.durable(QUEUE_VECTOR_GEN_REQ).build();
    }

    @Bean
    public Binding vectorGenReqBinding(Queue vectorGenReqQueue, TopicExchange aiTopicExchange) {
        return BindingBuilder.bind(vectorGenReqQueue).to(aiTopicExchange).with(ROUTING_KEY_VECTOR_GEN_REQ);
    }

    @Bean
    public Queue vectorGenResQueue() {
        return QueueBuilder.durable(QUEUE_VECTOR_GEN_RES).build();
    }

    @Bean
    public Binding vectorGenResBinding(Queue vectorGenResQueue, TopicExchange aiTopicExchange) {
        return BindingBuilder.bind(vectorGenResQueue).to(aiTopicExchange).with(ROUTING_KEY_VECTOR_GEN_RES);
    }
}
