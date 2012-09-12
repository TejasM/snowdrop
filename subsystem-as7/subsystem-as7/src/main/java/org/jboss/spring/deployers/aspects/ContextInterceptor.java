package org.jboss.spring.deployers.aspects;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	public Object interceptAndLog(ProceedingJoinPoint invocation)
			throws Throwable {
		Object[] args = invocation.getArgs();
		System.out.println("Finding Components from " + args[0]);
		if (SpringDeployment.index != null
				&& new Exception().getStackTrace()[3].getClassName().contains(
						"ComponentScanBeanDefinitionParser")) {
			classPathScanningObject = (ClassPathScanningCandidateComponentProvider) invocation
					.getThis();
			
			System.out.println("Using Custom Loader by scanning all classes matching base package");
			Object beans = findCandiateComponentsAlternate((String) args[0]);
			System.out.println(beans);
			return beans;
		} else {
			System.out.println("Unable to create the beans from Jandex");
			return invocation.proceed();
		}
	}
	
	/*
	 * Find Components but no support for meta-annotations (fastest but not viable)
	 */
	private Object findCandiateComponents(String basePackage) {
		Index index = SpringDeployment.index;
		List<ClassInfo> componentClasses = new ArrayList<ClassInfo>();

		List<AnnotationInstance> instances = getComponentClasses(index);

		for (AnnotationInstance annotationInstance : instances) {
			if (matchBase(((ClassInfo) annotationInstance.target()).toString(),
					basePackage))
				componentClasses.add((ClassInfo) annotationInstance.target());
		}
		return createBeanDefinitions(componentClasses);
	}

	/*
	 * Find Components with support for meta-annotations but only for those in the deployed project (fairly fast)
	 */
	private Object findCandiateComponentsTwoPass(String basePackage) {
		Index index = SpringDeployment.index;
		List<ClassInfo> componentClasses = new ArrayList<ClassInfo>();

		List<AnnotationInstance> instances = getComponentClasses(index);

		for (AnnotationInstance annotationInstance : instances) {
			if (matchBase(((ClassInfo) annotationInstance.target()).toString(),
					basePackage))
				componentClasses.add((ClassInfo) annotationInstance.target());
		}
		Set<BeanDefinition> beanSet = createBeanDefinitions(componentClasses);
		
		/*
		 * Second Pass
		 */
		// get Annotations marked as @Component
		List<ClassInfo> customComponentAnnotations = scanForMetaAnnotations(index);
		
		//Scan for the custom annotations
		scanForCustomComponents(beanSet, customComponentAnnotations, index, basePackage);
		return beanSet;
	}
	
	
	private void scanForCustomComponents(Set<BeanDefinition> beanSet,
			List<ClassInfo> customComponentAnnotations, Index index, String basePackage) {
		Set<BeanDefinition> beanDefs = new HashSet<BeanDefinition>();
		List<ClassInfo> componentClasses = new ArrayList<ClassInfo>();
		for (ClassInfo class1 : customComponentAnnotations){
			for(AnnotationInstance annotationInstance: index.getAnnotations(DotName.createSimple(class1.toString()))){
				if (matchBase(((ClassInfo) annotationInstance.target()).toString(),
						basePackage))
					componentClasses.add((ClassInfo) annotationInstance.target());
			}			
		}
		beanDefs = (Set<BeanDefinition>) createBeanDefinitions(componentClasses);
		beanSet.addAll(beanDefs);
	}

	private List<ClassInfo> scanForMetaAnnotations(Index index) {
		List<ClassInfo> metaAnnotations = new ArrayList<ClassInfo>();
		List<ClassInfo> list = new ArrayList<ClassInfo>();
		list.addAll(index.getKnownClasses());
		for (ClassInfo classInfo : list) {
			try {
				if((Class.forName(classInfo.name().toString(),
						false, classPathScanningObject.getResourceLoader()
								.getClassLoader()).isAnnotation())){
					Set<DotName> annotations = classInfo
							.annotations().keySet();
		OverallLoop: for (DotName entry : annotations) {
						for (AnnotationInstance t : classInfo.annotations().get(
								entry)) {
							if (isComponent(t.name().toString())) {
								metaAnnotations.add(classInfo);
								break OverallLoop;
							}
						}
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return metaAnnotations;		
	}

	/*
	 * Find Components with support for meta-annotations (slower but still almost 10 times faster than Spring's original 
	 */
	//TODO: Try and optimize a bit
	private Object findCandiateComponentsAlternate(String basePackage){
		Index index = SpringDeployment.index;
		List<ClassInfo> list = new ArrayList<ClassInfo>();
		list.addAll(index.getKnownClasses());
		List<ClassInfo> toRemove = new ArrayList<ClassInfo>();

		for (ClassInfo classInfo : list) {
			if (classInfo.annotations().isEmpty()) {
				toRemove.add(classInfo);
			}
		}
		list.removeAll(toRemove);
		toRemove.clear();
		for (ClassInfo classInfo : list) {
			if (!matchBase(classInfo.toString(), basePackage)) {
				toRemove.add(classInfo);
			}
		}
		list.removeAll(toRemove);
		toRemove.clear();
		List<ClassInfo> componentClasses = extractComponentClasses(list);
		return createBeanDefinitions(componentClasses);
	}

	private List<ClassInfo> extractComponentClasses(List<ClassInfo> list) {
		List<ClassInfo> componentClasses = new ArrayList<ClassInfo>();
		for (ClassInfo classInfo : list) {
			try {
				if((Class.forName(classInfo.name().toString(),
						false, classPathScanningObject.getResourceLoader()
								.getClassLoader()).isAnnotation())){
					continue;
				}
				Set<DotName> annotations = classInfo
						.annotations().keySet();
	ComponentLoop: for (DotName entry : annotations) {
					for (AnnotationInstance t : classInfo.annotations().get(
							entry)) {
							if (isComponent(t.name().toString())) {
								if(!(Class.forName(classInfo.name().toString(),
											false, classPathScanningObject.getResourceLoader()
													.getClassLoader()).isAnnotation())){
									componentClasses.add(classInfo);
									break ComponentLoop;
								}
							} else {
									Class<?> cls = Class.forName(t.name().toString(),
											false, classPathScanningObject.getResourceLoader()
											.getClassLoader());
									if (cls.isAnnotation()) {
										Annotation[] annos = cls.getAnnotations();
										for (int i = 0; i < annos.length; i++) {
											if (isComponent(annos[i]
													.annotationType().toString())) {
												componentClasses.add(classInfo);
												break ComponentLoop;											
											}
										}
									} else {
									
									}
								}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return componentClasses;
	}

	private boolean isComponent(String string) {
		if(string.startsWith("interface ")){
			string = string.substring("interface ".length());
		}
		return string.startsWith("org.springframework.stereotype.") &&
				(string.endsWith("Component") || string.endsWith("Repository") || string.endsWith("Service") 
					|| string.endsWith("Controller"));
	}


	private List<AnnotationInstance> getComponentClasses(Index index) {
		List<AnnotationInstance> annoatatedInstances = new ArrayList<AnnotationInstance>();
		annoatatedInstances.addAll(index.getAnnotations(DotName
				.createSimple("org.springframework.stereotype.Repository")));
		annoatatedInstances.addAll(index.getAnnotations(DotName
				.createSimple("org.springframework.stereotype.Service")));
		annoatatedInstances.addAll(index.getAnnotations(DotName
				.createSimple("org.springframework.stereotype.Controller")));
		annoatatedInstances.addAll(index.getAnnotations(DotName
				.createSimple("org.springframework.stereotype.Component")));
		return annoatatedInstances;
	}

	private boolean matchBase(String string, String basePackage) {
		String[] splitBase = basePackage.split("\\.");
		String[] splitClass = string.split("\\.");
		if (splitBase.length > splitClass.length) {
			return false;
		}
		for (int i = 0; i < splitBase.length; i++) {
			if (splitBase[i].equals(splitClass[i])) {
				continue;
			} else if (splitBase[i].equals("*")) {
				continue;
			} else if (splitBase[i].equals("**")) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	private Set<BeanDefinition> createBeanDefinitions(List<ClassInfo> componentClasses) {
		Set<BeanDefinition> beanDefs = new HashSet<BeanDefinition>();

		for (ClassInfo class1 : componentClasses) {
			String classPath = ResourcePatternResolver.CLASSPATH_URL_PREFIX
					+ resolveBasePackage(class1.toString()) + ".class";
			ResourcePatternResolver resourcePatternResolver = (ResourcePatternResolver) classPathScanningObject
					.getResourceLoader();
			try {
				Resource resource = resourcePatternResolver
						.getResource(classPath);
				GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
				beanDefinition.setResource(resource);
				beanDefinition.setBeanClass(Class.forName(class1.toString(),
						false, classPathScanningObject.getResourceLoader()
								.getClassLoader()));
				beanDefs.add(beanDefinition);
			} catch (ClassNotFoundException e) {
				System.out.println(class1.toString() + " not found");
				continue;
			}
		}
		return beanDefs;
	}

	private String resolveBasePackage(String string) {
		return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils
				.resolvePlaceholders(string));
	}

}
