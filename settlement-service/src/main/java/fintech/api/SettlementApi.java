package fintech.api;


import fintech.application.SettlementService;
import fintech.domain.entity.Settlement;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementApi {

    private final SettlementService settlementService;

    //특정 가맹점의 특정 날짜 정산 내역 조회
    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<Settlement> getSettlement(
            @PathVariable String merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime date) {

        Settlement response = settlementService.getSettlement(merchantId, date);
        return ResponseEntity.ok(response);
    }
}
