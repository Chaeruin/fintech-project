package fintech.domain.service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class SettlementCalculator {

    // 수수료율 설정 - 기본 3%로 가정
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.03");

    /**
     * 총 결제 금액에 대한 수수료 계산
     * @param totalAmount 총 결제 금액
     * @return 계산된 수수료 (소수점 이하 반올림)
     */
    public BigDecimal calculateFee(BigDecimal totalAmount) {
        return totalAmount.multiply(DEFAULT_FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 실제 정산 금액 계산 (결제 금액 - 수수료)
     * @param totalAmount 총 결제 금액
     * @param fee 계산된 수수료
     * @return 가맹점에게 입금해줄 최종 정산 금액
     */
    public BigDecimal calculateSettlementAmount(BigDecimal totalAmount, BigDecimal fee) {
        return totalAmount.subtract(fee);
    }
}
