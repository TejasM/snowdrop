package org.jboss.spring.deployers.aspects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.spring.deployers.as7.SpringDeployment;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;


@Aspect
public class ContextInterceptor {
	private ClassPathScanningCandidateComponentProvider classPathScanningObject;

	@Around("execution(public * org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider.findCandidateComponents(String))")
	public Object interceptAndLog(ProceedingJoinPoint invocation) throws Throwable {
		Object[] args = invocation.getArgs();
		if(SpringDeployment.index!=null && new Exception().getStackTrace()[3].getClassName().contains("ComponentScanBeanDefinitionParser")){
			classPathScanningObject = (ClassPathScanningCandidateComponentProvider) invocation.getThis();
			Object beans = findCandiateComponents((String) args[0]);
/*			System.out.println(beans);
			
			System.out.println("Now using Orginal Method");
			beans = invocation.proceed();
			System.out.println(beans);*/
			return beans;
		}
		else{
			System.out.println("Unable to create the beans from Jandex");
			return invocation.proceed();
		}
	}
	

	private Object findCandiateComponents(String basePackage) {		
		Index index = SpringDeployment.index;
		List<ClassInfo> componentClasses = new ArrayList<ClassInfo>();
		
		List<AnnotationInstance> instances = getComponentClasses(index);
		for (AnnotationInstance annotationInstance : instances) {
			if(matchBase(((ClassInfo) annotationInstance.target()).toString(), basePackage))
				componentClasses.add((ClassInfo) annotationInstance.target());
		}
		return createBeanDefinitions(componentClasses);
	}
	
	private List<AnnotationInstance> getComponentClasses(Index index){
		List<AnnotationInstance> annoatatedInstances = new ArrayList<AnnotationInstance>();
		annoatatedInstances.addAll(index.getAnnotations(DotName.createSimple("org.springframework.stereotype.Repository")));
		annoatatedInstances.addAll(index.getAnnotations(DotName.createSimple("org.springframework.stereotype.Service")));
		annoatatedInstances.addAll(index.getAnnotations(DotName.createSimple("org.springframework.stereotype.Controller")));
		annoatatedInstances.addAll(index.getAnnotations(DotName.createSimple("org.springframework.stereotype.Component")));
		return annoatatedInstances;
	}


	private boolean matchBase(String string, String basePackage) {
		String[] splitBase = basePackage.split("\\.");
		String[] splitClass = string.split("\\.");
		if(splitBase.length > splitClass.length){
			return false;
		}
		for (int i = 0; i < splitBase.length; i++) {
			if(splitBase[i].equals(splitClass[i])){
				continue;
			}else if(splitBase[i].equals("*")){
				continue;
			}else if(splitBase[i].equals("**")){
				return true;
			}else{
				return false;
			}
		}
		return true;
	}


	private Object createBeanDefinitions(List<ClassInfo> componentClasses) {
		Set<BeanDefinition> beanDefs = new HashSet<BeanDefinition>();
		
		for (ClassInfo class1 : componentClasses) {
			String classPath = ResourcePatternResolver.CLASSPATH_URL_PREFIX +
					resolveBasePackage(class1.toString()) + ".class";
			System.out.println(classPathScanningObject.getResourceLoader().getClass());
			ResourcePatternResolver resourcePatternResolver = (ResourcePatternResolver) classPathScanningObject.getResourceLoader();
			try {				
				Resource resource = resourcePatternResolver.getResource(classPath);
				GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
				beanDefinition.setResource(resource);
				beanDefinition.setBeanClass(Class.forName(class1.toString(), false,  classPathScanningObject.getResourceLoader().getClassLoader()));
				beanDefs.add(beanDefinition);
			} catch (ClassNotFoundException e) {
				System.out.println(class1.toString() + " not found");
				continue;
			}
		}
		return beanDefs;
	}

	private String resolveBasePackage(String string) {
		return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(string));
	}
	

}
