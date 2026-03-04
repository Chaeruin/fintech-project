package fintech;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "fintech.payment.common.domain")
@EnableJpaRepositories(basePackages = "fintech.settlement.service.infrastructure.persistence")
public class PaymentSettlementApplication { //

    public static void main(String[] args) {
        SpringApplication.run(PaymentSettlementApplication.class, args);
    }

}