package com.application.jokester.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Aspect
@Component
public class DataSourceRoutingAspect {

    // ─── Intercepts every method annotated with @ReadOnly ────────────────
    // Before method executes → switch to REPLICA
    // After method finishes → switch back to PRIMARY
    @Around("@annotation(com.application.jokester.config.datasource.ReadOnly)")
    public Object routeToReplica(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.debug("Routing to REPLICA for: {}", joinPoint.getSignature().getName());
            DataSourceContextHolder.setDataSourceType(DataSourceType.REPLICA);
            return joinPoint.proceed();
        } finally {
            // ALWAYS clear — even if method throws exception
            DataSourceContextHolder.clearDataSourceType();
            log.debug("Cleared datasource routing");
        }
    }

    // ─── Intercepts @Transactional(readOnly=true) automatically ──────────
    // No need to add @ReadOnly manually if you already use readOnly=true
    @Around("@annotation(transactional) && @annotation(transactional)")
    public Object routeReadOnlyTransaction(ProceedingJoinPoint joinPoint,
                                           Transactional transactional) throws Throwable {
        if (transactional.readOnly()) {
            try {
                DataSourceContextHolder.setDataSourceType(DataSourceType.REPLICA);
                return joinPoint.proceed();
            } finally {
                DataSourceContextHolder.clearDataSourceType();
            }
        }
        return joinPoint.proceed();
    }
}