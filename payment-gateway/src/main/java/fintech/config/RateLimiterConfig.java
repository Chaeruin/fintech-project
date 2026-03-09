package fintech.config;

import java.net.InetSocketAddress;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // 원격 주소를 가져와서 IP만 추출
            return Mono.just(
                    Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(InetSocketAddress::getHostName)
                            .orElse("unknown")
            );
        };
    }
}