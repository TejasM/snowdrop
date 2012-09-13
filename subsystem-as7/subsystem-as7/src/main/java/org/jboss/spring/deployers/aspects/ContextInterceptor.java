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

@Aspect
public class ContextInterceptor {
	private ClassPathScanningCandidateComponentProvider classPathScanningObject;

	/*
	 * Aspect to intercept spring's BeanDefinition for component scanning
	 */
	@Around("execution(public * org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider.findCandidateComponents(String))")
	public Object interceptAndLog(ProceedingJoinPoint invocation)
			throws Throwable {
		long time;
		Object[] args = invocation.getArgs();
		System.out.println("Finding Components from " + args[0]);
		if (SpringDeployment.index != null
				&& new Exception().getStackTrace()[3].getClassName().contains(
						"ComponentScanBeanDefinitionParser")) {
			classPathScanningObject = (ClassPathScanningCandidateComponentProvider) invocation
					.getThis();
			
			time = System.currentTimeMillis();
			Object beans = invocation.proceed();
			System.out.println((System.currentTimeMillis()-time));
			System.out.println("Using Original");

			
			time = System.currentTimeMillis();
			beans = findCandiateComponents((String) args[0]);
			System.out.println(System.currentTimeMillis()-time);
			System.out.println("Using Custom Loader without Support for Meta Annotations");
			
			time = System.currentTimeMillis();
			beans = findCandiateComponentsAlternate((String) args[0]);
			System.out.println(System.currentTimeMillis()-time);
			System.out.println("Using Custom Loader by scanning all classes matching base package");

			
			time = System.currentTimeMillis();
			beans = findCandiateComponentsTwoPass((String) args[0]);
			System.out.println(System.currentTimeMillis()-time);
			System.out.println("Using Custom Loader through Two Pass");
			
			return beans;
		} else {
			System.out.println("Unable to create the beans from Jandex");
			return invocation.proceed();
		}
	}
	
	/*
	 * Meta Annotations not supported (fastest)
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
	 * Two Pass Scanning to scan for meta annotations that are in the deployment project
	 * (Fast, almost as much as with no support for meta-annotations)
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
		//Scan for those
		scanForCustomComponents((Set<BeanDefinition>) beanSet, customComponentAnnotations, index, basePackage);
		return beanSet;
	}
	
	
	/*
	 * Scan the index for the components declared via meta-annotations
	 */
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
		beanDefs = createBeanDefinitions(componentClasses);
		beanSet.addAll(beanDefs);
	}
	
	/*
	 * Scan the project for custom annotations
	 */
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
							if (delveIntoAnnotation(t.name().toString(), new ArrayList<String>())) {
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
	 * Full meta-annotation support (Slower than two pass but not still at least 10x the speed of Spring's scan)
	 */
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

	/*
	 * Extract all the components by looking through all annotated classes
	 * and searching for any stereotype annotations
	 */
	private List<ClassInfo> extractComponentClasses(List<ClassInfo> list) {
		List<ClassInfo> componentClasses = new ArrayList<ClassInfo>();
		for (ClassInfo classInfo : list) {
			if (classInfo.annotations() != null) {
				Set<DotName> annotations = classInfo
						.annotations().keySet();
	OverallLoop: for (DotName entry : annotations) {
					for (AnnotationInstance t : classInfo.annotations().get(
							entry)) {
						try {
							if (isComponent(t.name().toString())) {
								if(!(Class.forName(classInfo.name().toString(),
											false, classPathScanningObject.getResourceLoader()
													.getClassLoader()).isAnnotation())){
									componentClasses.add(classInfo);
									break OverallLoop;
								}
							} else {
									Class<?> cls = Class.forName(t.name().toString(),
											false, classPathScanningObject.getResourceLoader()
											.getClassLoader());
									if (cls.isAnnotation()) {
										Annotation[] annos = cls.getAnnotations();
										for (int i = 0; i < annos.length; i++) {
											if(delveIntoAnnotation(annos[i]
													.annotationType().toString(), new ArrayList<String>())){
												componentClasses.add(classInfo);
												break OverallLoop;	
											}
										}
									} else {
									
									}
								}
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return componentClasses;
	}
	
	/*
	 * Traverse the annotation "tree" to find if a stereotype annotation is somewhere within
	 */
	private boolean delveIntoAnnotation(String string, List<String> annotationGraph) {
		if(string.startsWith("interface ")){
			string = string.substring("interface ".length());
		}
		try {
			Class<?> annotation = Class.forName(string,
					false, classPathScanningObject.getResourceLoader()
					.getClassLoader());
			if(isComponent(annotation.getName())){
				return true;
			}
			if(javaMetaAnnotation(annotation.getName()) || annotationGraph.contains(annotation.getName())){
				return false;					
			}
			annotationGraph.add(annotation
					.getName());
			Annotation[] annotations = annotation.getAnnotations();
			for (Annotation annotation2 : annotations) {
				if(javaMetaAnnotation(annotation2
						.annotationType().toString())){
					continue;
				}
				if(annotationGraph.contains(annotation2
						.annotationType().toString())){
					continue;					
				}
				annotationGraph.add(annotation
						.getName());
				if(isComponent(annotation2
						.annotationType().toString())){
					return true;
				}else{
					if(delveIntoAnnotation(annotation2
						.annotationType().toString(), annotationGraph)){
						return true;
					}
				}
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * Simple optimization function that detects the four main java meta-annotations
	 */
	private boolean javaMetaAnnotation(String string) {
		if(string.endsWith("Retention") || string.endsWith("Documented") ||
				string.endsWith("Inherited") || string.endsWith("Target")){
			return true;
		}
		return false;
	}

	private boolean isComponent(String string) {
		if(string.startsWith("interface ")){
			string = string.substring("interface ".length());
		}
		return string.startsWith("org.springframework.stereotype.") &&
				(string.endsWith("Component") || string.endsWith("Repository") || string.endsWith("Service") 
					|| string.endsWith("Controller"));
	}

	/*
	 * Get the standard components
	 */
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
	/*
	 * Check if the class package being checked for matches that of the basePackage
	 */
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
	
	/*
	 * Create the actual bean definitions 
	 */
	private Set<BeanDefinition> createBeanDefinitions(List<ClassInfo> componentClasses) {
		Set<BeanDefinition> beanDefs = new HashSet<BeanDefinition>();

		for (ClassInfo class1 : componentClasses) {
			try {
				GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
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

}
