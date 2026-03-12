package fintech.domain.enums;

public enum SettlementStatus {
    READY,      // 정산 데이터 생성됨
    COMPLETED,  // 가맹점 계좌 입금 완료
    FAILED      // 정산 처리 실패
}