package fintech.aop;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();                           // 락의 이름 (SpEL 지원)
    long waitTime() default 5L;             // 락 획득 대기 시간
    long leaseTime() default 10L;           // 락 점유 시간
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
