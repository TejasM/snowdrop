package org.jboss.spring.factory;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.core.io.Resource;

public class CustomXmlApplicationListener implements ApplicationListener {
	
	private ConfigurableApplicationContext context;
	
	private Resource resource;
	
	
	public CustomXmlApplicationListener(Resource resource) {
		super();
		this.resource = resource;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		this.context= (ConfigurableApplicationContext) ((ApplicationContextEvent) event).getApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) this.context.getAutowireCapableBeanFactory());
		reader.loadBeanDefinitions(resource);	
	}

}
