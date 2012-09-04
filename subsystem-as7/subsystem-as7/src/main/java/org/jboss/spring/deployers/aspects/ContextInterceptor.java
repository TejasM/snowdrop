package org.jboss.spring.deployers.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;


@Aspect
public class ContextInterceptor {
	
	@Around("execution(public * org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider.findCandidateComponents(String))")
	public Object interceptAndLog(ProceedingJoinPoint invocation) throws Throwable {
		System.out.println("In the point cut");
		return invocation.proceed();
	}

}
