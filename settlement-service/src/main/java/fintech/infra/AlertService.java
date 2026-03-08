package fintech.infra;

import fintech.common.infra.AlertProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertService implements AlertProvider {
    @Override
    public void sendAlert(String message) {

    }
}
