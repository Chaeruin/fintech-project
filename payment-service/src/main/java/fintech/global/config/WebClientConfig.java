package fintech.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${payment.toss.base-url}")
    private String tossBaseUrl;

    @Value("${payment.portone.base-url}")
    private String portoneBaseUrl;

    /* 토스페이먼츠 전용 WebClient */
    @Bean
    public WebClient tossPaymentWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(tossBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // 여기에 PG사 API 키 인증 헤더를 추가 가능
                .build();
    }

    /* 포트원(구 아임포트) 전용 WebClient */
    @Bean
    public WebClient portonePaymentWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(portoneBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
