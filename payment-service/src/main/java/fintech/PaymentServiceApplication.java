package fintech;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;


@SpringBootApplication
@EntityScan(basePackages = "fintech.payment.common.domain")
public class PaymentServiceApplication { //

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}