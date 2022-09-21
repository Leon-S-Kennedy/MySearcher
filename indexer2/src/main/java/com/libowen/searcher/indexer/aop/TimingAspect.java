package com.libowen.searcher.indexer.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class TimingAspect {
    @Pointcut("@annotation(com.libowen.searcher.indexer.aop.Timing)")
    public void timingPointCut(){}
    @Around("timingPointCut()")
    public Object timing(ProceedingJoinPoint joinPoint) throws Throwable{

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Timing annotation = method.getAnnotation(Timing.class);
        String value = annotation.value();

        long b = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        }finally {
            long e = System.currentTimeMillis();
            double ms =e*1.0-b;
            double s=ms/1000;
            log.info("{}耗时{}秒",value,s);
        }
    }
}
