package xyz.imaketech.holdmyseat.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class CacheConfig {

    /** 10 min TTL plus up to 2 min jitter to avoid stampedes. */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("catalog:")
                .disableCachingNullValues()
                .entryTtl((key, value) -> Duration.ofMinutes(10)
                        .plusSeconds(ThreadLocalRandom.current().nextInt(120)));
    }
}
