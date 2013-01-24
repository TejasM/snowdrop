/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.spring.factory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

import org.jboss.util.naming.Util;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:ales.justin@genera-lynx.com">Ales Justin</a>
 */

public class NamedApplicationContext implements ConfigurableApplicationContext, Nameable {

	private String defaultName;

	private String name;

	protected ConfigurableApplicationContext context;

	public NamedApplicationContext(ConfigurableApplicationContext context, String defaultName) {
		this.defaultName = defaultName;
		this.context = context;
	}

	public void initializeName(String ... names) {
		String name = names[0];
		if (name == null || "".equals(StringUtils.trimAllWhitespace(name))) {
			name = defaultName;			
			((AbstractApplicationContext) context).setDisplayName(name);
		}
		this.name = name;
		if (names.length>1){
			try {
                this.context.getBeanFactory().setParentBeanFactory((BeanFactory) Util.lookup(names[1], BeanFactory.class));
            } catch (Exception e) {
                throw new BeanDefinitionStoreException("Failure during parent bean factory JNDI lookup: " + names[1], e);
            }
		}
		((AbstractApplicationContext) context).setDisplayName(name);
		context.refresh();
	}

	@Override
	public String getId() {
		return this.context.getId();
	}

	@Override
	public String getDisplayName() {
		return this.context.getDisplayName();
	}

	@Override
	public long getStartupDate() {
		return this.context.getStartupDate();
	}

	@Override
	public ApplicationContext getParent() {
		return this.context.getParent();
	}

	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
			throws IllegalStateException {
		return this.context.getAutowireCapableBeanFactory();
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return this.context.containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.context.getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return this.context.getBeanDefinitionNames();
	}

	@Override
	public String[] getBeanNamesForType(Class type) {
		return this.context.getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(Class type,
			boolean includeNonSingletons, boolean allowEagerInit) {
		return this.context.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}


	@Override
	public Object getBean(String name) throws BeansException {
		return this.context.getBean(name);
	}

	@Override
	public Object getBean(String name, Object[] args) throws BeansException {
		return this.context.getBean(name, args);
	}

	@Override
	public boolean containsBean(String name) {
		return this.context.containsBean(name);
	}

	@Override
	public boolean isSingleton(String name)
			throws NoSuchBeanDefinitionException {
		return this.context.isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name)
			throws NoSuchBeanDefinitionException {
		return this.context.isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, Class targetType)
			throws NoSuchBeanDefinitionException {
		return this.context.isTypeMatch(name, targetType);
	}

	@Override
	public Class getType(String name) throws NoSuchBeanDefinitionException {
		return this.context.getType(name);
	}

	@Override
	public String[] getAliases(String name) {
		return this.context.getAliases(name);
	}

	@Override
	public BeanFactory getParentBeanFactory() {
		return this.context.getParentBeanFactory();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return this.context.containsLocalBean(name);
	}

	@Override
	public String getMessage(String code, Object[] args, String defaultMessage,
			Locale locale) {
		return this.context.getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale)
			throws NoSuchMessageException {
		return this.context.getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		return this.context.getMessage(resolvable, locale);
	}

	@Override
	public void publishEvent(ApplicationEvent event) {
		this.context.publishEvent(event);
	}

	@Override
	public Resource[] getResources(String arg0) throws IOException {
		return this.context.getResources(arg0);
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.context.getClassLoader();
	}

	@Override
	public Resource getResource(String arg0) {
		return this.context.getResource(arg0);
	}

	@Override
	public void start() {
		this.context.start();
	}

	@Override
	public void stop() {
		this.context.stop();
	}

	@Override
	public boolean isRunning() {
		return this.context.isRunning();
	}

	@Override
	public void setParent(ApplicationContext parent) {
		this.context.setParent(parent);
	}

	@Override
	public void addBeanFactoryPostProcessor(
			BeanFactoryPostProcessor beanFactoryPostProcessor) {
		this.context.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
	}

	@Override
	public void addApplicationListener(ApplicationListener listener) {
		this.context.addApplicationListener(listener);
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		this.context.refresh();
	}

	@Override
	public void registerShutdownHook() {
		this.context.registerShutdownHook();
	}

	@Override
	public void close() {
		this.context.close();
	}

	@Override
	public boolean isActive() {
		return this.context.isActive();
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory()
			throws IllegalStateException {
		return this.context.getBeanFactory();
	}

	@Override
	public String getName() {
		String name = this.name != null? this.name : defaultName;
        if (name == null) {
            throw new IllegalArgumentException("Bean factory JNDI name must be set!");
        }
        return name;
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String arg0,
			Class<A> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> arg0)
			throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> arg0, boolean arg1,
			boolean arg2) throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(
			Class<? extends Annotation> arg0) throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getBean(Class<T> arg0) throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getBean(String arg0, Class<T> arg1) throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setId(String arg0) {
		// TODO Auto-generated method stub
		
	}
}