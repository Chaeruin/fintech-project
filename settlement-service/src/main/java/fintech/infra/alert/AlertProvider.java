package fintech.infra.alert;

public interface AlertProvider {
    void sendAlert(String message);
}
