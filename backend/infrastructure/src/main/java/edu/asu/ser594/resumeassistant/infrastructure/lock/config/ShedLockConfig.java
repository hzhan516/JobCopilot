package edu.asu.ser594.resumeassistant.infrastructure.lock.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock 分布式定时任务锁配置 / ShedLock distributed scheduler lock configuration
 * <p>
 * 确保多实例部署时，@Scheduled 定时任务仅在一个实例上执行。
 * Ensures that @Scheduled tasks run on only one instance in a multi-instance deployment.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "ra:shedlock");
    }
}
