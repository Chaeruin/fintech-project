package fintech.api;

import fintech.application.PaymentService;
import fintech.application.dto.PaymentConfirmCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApi {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPayment(
            @RequestBody PaymentConfirmCommand command
    ) {
        paymentService.completePayment(command.paymentKey(), command);
        return ResponseEntity.ok("결제가 성공적으로 완료되었습니다.");
    }
}
