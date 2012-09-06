/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.spring.deployers.as7;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.spring.factory.NamedApplicationContext;
import org.jboss.spring.util.BasePackageParserImpl;
import org.jboss.spring.util.JndiParse;
import org.jboss.spring.util.PropsJndiParse;
import org.jboss.spring.util.XmlJndiParse;
import org.jboss.spring.vfs.VFSResource;
import org.jboss.spring.vfs.context.VFSClassPathXmlApplicationContext;
import org.jboss.vfs.VirtualFile;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Marius Bogoevici
 */
public class SpringBootstrapProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        SpringDeployment locations = SpringDeployment.retrieveFrom(phaseContext.getDeploymentUnit());
        if (locations == null) {
            return;
        }
        String springVersion = locations.getSpringVersion();
        String internalJndiName;
        for (VirtualFile virtualFile : locations.getContextDefinitionLocations()) {

            final EEModuleDescription moduleDescription = phaseContext.getDeploymentUnit() .getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            ConfigurableApplicationContext applicationContext;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            boolean hasNamespaceContextSelector = moduleDescription != null && moduleDescription.getNamespaceContextSelector() != null;
            try {
                Thread.currentThread().setContextClassLoader(phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE).getClassLoader());
                if (hasNamespaceContextSelector) {
                    NamespaceContextSelector.pushCurrentSelector(moduleDescription.getNamespaceContextSelector());
                }
                
                applicationContext = setupApplicationContext(springVersion, virtualFile, phaseContext);
                if(applicationContext==null){
                	continue;
                }
                internalJndiName = applicationContext.getDisplayName();
            }catch (Exception e) {
            	e.printStackTrace();
				throw new RuntimeException();
			} finally {
                Thread.currentThread().setContextClassLoader(cl);
                if (hasNamespaceContextSelector) {
                    NamespaceContextSelector.popCurrentSelector();
                }
            }
            ApplicationContextService service = new ApplicationContextService(applicationContext);
            ServiceName serviceName = phaseContext.getDeploymentUnit().getServiceName().append(internalJndiName);
            ServiceBuilder<?> serviceBuilder = serviceTarget.addService(serviceName, service);
            serviceBuilder.install();
            String jndiName = JndiName.of("java:jboss").append(internalJndiName).getAbsoluteName();
            int index = jndiName.indexOf("/");
            String namespace = (index > 5) ? jndiName.substring(5, index) : null;
            String binding = (index > 5) ? jndiName.substring(index + 1) : jndiName.substring(5);
            ServiceName naming = (namespace != null) ? ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(namespace) : ContextNames.JAVA_CONTEXT_SERVICE_NAME;
            ServiceName bindingName = naming.append(binding);
            BinderService binder = new BinderService(binding);
            InjectedValue<ApplicationContext> injectedValue = new InjectedValue<ApplicationContext>();
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            serviceTarget.addService(bindingName, binder)
                    .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                    .addInjection(binder.getManagedObjectInjector(), new ValueManagedReferenceFactory(injectedValue))
                    .addDependency(serviceName, ApplicationContext.class, injectedValue)
                    .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                    .install();
        }
    }
    
    private ConfigurableApplicationContext setupApplicationContext(String springVersion, VirtualFile virtualFile, DeploymentPhaseContext phaseContext) throws ClassNotFoundException {
    	ConfigurableApplicationContext applicationContext;
    	if (virtualFile.getPathName().endsWith(".xml")) {
			String name = phaseContext.getDeploymentUnit().getName();

			if("".equals(SpringDeployment.retrieveFrom(phaseContext.getDeploymentUnit()).getXmlApplicationContext())){
				applicationContext = xmlApplicationContext(springVersion,
					virtualFile);
				setJndiName(new XmlJndiParse(), virtualFile, applicationContext, name);
			}else{
				applicationContext = customXmlApplicationContext(SpringDeployment.retrieveFrom(phaseContext.getDeploymentUnit()), virtualFile);
				setJndiName(new XmlJndiParse(), virtualFile, applicationContext, name);
			}
			
		} else {
			if (springVersion.equals("3.0+")) {
				try {
					/*
					 * Reflection for AnnotationApplicationContext
					 */
					applicationContext = annotationApplicationContext(virtualFile);
					String name = phaseContext.getDeploymentUnit().getName();
					
					setJndiName(new PropsJndiParse(),
							virtualFile, applicationContext, name);

				} catch (Throwable e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
			} else {
				return null;
			}

		}
		return applicationContext;
	}

	private ConfigurableApplicationContext xmlApplicationContext(
			String springVersion, VirtualFile virtualFile) {
		ConfigurableApplicationContext applicationContext;
		
			if (springVersion.equals("3.0+")) {						
				try {
					applicationContext = new ClassPathXmlApplicationContext((new VFSResource (virtualFile)).getURL().toString());
				} catch (BeansException e) {
					System.out.println("Error in file: " + virtualFile.getName());
					return null;
				} catch (IOException e) {
					return null;
				}
			} else {
				applicationContext = new VFSClassPathXmlApplicationContext(
						new String[] {}, false);
				((VFSClassPathXmlApplicationContext) applicationContext).setResource(new VFSResource(virtualFile));
			}
			
		return applicationContext;
	}
	
	private ConfigurableApplicationContext customXmlApplicationContext(SpringDeployment springDeployment, VirtualFile virtualFile) throws ClassNotFoundException {
		ConfigurableApplicationContext applicationContext;
		try{
			Class<?> xmlApplicationContext = Class
					.forName(springDeployment.getXmlApplicationContext());
			Constructor<?> ct = xmlApplicationContext
					.getConstructor(new Class[] {String.class});
			String resourceLocation = (new VFSResource(virtualFile)).getURL().toString();
			applicationContext = (ConfigurableApplicationContext) ct.newInstance(new Object[]{resourceLocation});			
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR: XmlApplicationContext specified could not be found");
			throw new ClassNotFoundException();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("ERROR: Please use a valid xml application context, i.e. one that implements ClassPathApplicatonContext");
			throw new RuntimeException();
		}
		return applicationContext;
	}

	private NamedApplicationContext setJndiName(JndiParse propsParser,
			VirtualFile virtualFile,
			ConfigurableApplicationContext applicationContext, String name) {
		NamedApplicationContext namedContext;
		namedContext = new NamedApplicationContext(
				applicationContext, name);
		namedContext.initializeName(propsParser
				.getJndiName(new VFSResource(virtualFile)));
		return namedContext;
	}

	private ConfigurableApplicationContext annotationApplicationContext(
			VirtualFile virtualFile) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		ConfigurableApplicationContext applicationContext;
		Class<?> annotationApplicationContext = Class
				.forName("org.springframework.context.annotation.AnnotationConfigApplicationContext");
		Constructor<?> ct = annotationApplicationContext
				.getConstructor();
		applicationContext = (ConfigurableApplicationContext) ct.newInstance();
		String[] basePackages = (new BasePackageParserImpl())
				.parseBasePackages(new VFSResource(
						virtualFile));						
		Method methodScan = annotationApplicationContext.getDeclaredMethod("scan", String[].class);
		methodScan.invoke(annotationApplicationContext.cast(applicationContext), new Object[]{basePackages});
		return applicationContext;
	}

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
