package com.chieaid24.device_service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {
  /** Pointcut that matches all methods in controller package */
  @Pointcut("execution(* com.chieaid24.device_service.controller.*.*(..))")
  public void controllerMethods() {}

  @Around("controllerMethods()")
  public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.nanoTime();
    try {
      return pjp.proceed();
    } finally {
      long end = System.nanoTime();
      long duration = end - start;
      log.info("Execution time of {}: {} ms", pjp.getSignature().getName(), duration / 1_000_000);
    }
  }
}
