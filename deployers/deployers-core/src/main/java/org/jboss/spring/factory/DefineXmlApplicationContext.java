package org.jboss.spring.factory;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

public class DefineXmlApplicationContext extends
		NamedApplicationContext {

	
	Resource resource;
	
	public DefineXmlApplicationContext(
			ConfigurableApplicationContext context, String defaultName, Resource resource) {
		super(context, defaultName);
		this.resource = resource;
	}
	
	@Override
	public void initializeName(String... names) {
		// TODO Auto-generated method stub
		super.initializeName(names);
		
	}

}
