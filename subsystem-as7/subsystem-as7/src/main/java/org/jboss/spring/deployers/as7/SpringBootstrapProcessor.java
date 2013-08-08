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
import org.jboss.logging.Logger;
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
import org.jboss.vfs.VirtualFile;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;

/**
 * @author Marius Bogoevici
 */
public class SpringBootstrapProcessor implements DeploymentUnitProcessor {

    private String xmlApplicationContext;

    private static final Logger log = Logger.getLogger("org.jboss.snowdrop");

    public SpringBootstrapProcessor(String xmlApplicationContext) {
        this.xmlApplicationContext = xmlApplicationContext;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        // Retrieve Locations from the SpringDeployment we created in @see SpringStrutureProcessor
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

    /**
     *  Bootstrap the applicationcontext based the spring version, the resource, and the custom configurable context
     * @param springVersion: the spring version during runtime
     * @param virtualFile: The resource file used bootstrap the applicationcontext
     * @param phaseContext: the used to get the determine the type of applicationcontext and the deployment name
     * @return
     * @throws ClassNotFoundException
     */
    private ConfigurableApplicationContext setupApplicationContext(String springVersion, VirtualFile virtualFile, DeploymentPhaseContext phaseContext) throws ClassNotFoundException {
    	ConfigurableApplicationContext applicationContext;
    	if (virtualFile.getPathName().endsWith(".xml")) {
			String name = phaseContext.getDeploymentUnit().getName();
            // If there is not custom application context defined set to the standard ClassPathXmlApplicationContext
			if("".equals(this.xmlApplicationContext)){
                this.xmlApplicationContext = "org.springframework.context.support.ClassPathXmlApplicationContext";
			}
            applicationContext = xmlApplicationContext(virtualFile);
            setJndiName(new XmlJndiParse(), virtualFile, applicationContext, name);
            log.info("Created " +  this.xmlApplicationContext + " from " + virtualFile.getName());
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
					throw new RuntimeException();
				}
			} else {
				log.error("Wrong Version of Spring if using a Property File");
                throw new RuntimeException();
			}
            log.info("Created AnnotationApplicationContext from " + virtualFile.getName());
        }
		return applicationContext;
	}

    /**
     * Create a ConfigurableApplicationContext from the xml resource: virtualFile and xml Virtual File
     * @param virtualFile
     * @return
     * @throws ClassNotFoundException
     */
	private ConfigurableApplicationContext xmlApplicationContext(VirtualFile virtualFile) throws ClassNotFoundException {
		ConfigurableApplicationContext applicationContext;
		try{
			Class<?> xmlApplicationContext = Class
					.forName(this.xmlApplicationContext);
			Constructor<?> ct = xmlApplicationContext
					.getConstructor();
			String resourceLocation = (new VFSResource(virtualFile)).getURL().toString();
			applicationContext = (ConfigurableApplicationContext) ct.newInstance();
            ((AbstractRefreshableConfigApplicationContext)applicationContext).setConfigLocations(new String[]{resourceLocation});
		} catch (ClassNotFoundException e) {
            e.printStackTrace();
			log.error("XmlApplicationContext specified could not be found");
			throw new ClassNotFoundException();
		} catch (ClassCastException e) {
			e.printStackTrace();
			log.error("Please use a valid xml application context, i.e. one that implements AbstractRefreshableConfigApplicationContext");
			throw e;
		} catch (NoSuchMethodException e) {
            e.printStackTrace();
            log.error("Ensure that the application context you are trying to use has a default empty constructor");
            throw new RuntimeException();
        } catch (InstantiationException e) {
            e.printStackTrace();
            log.error("Ensure that the application context class is a concrete class (i.e. not an interface or abstract)");
            throw new RuntimeException();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            log.error("Ensure that the application context's constructor is public");
            throw new RuntimeException();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Could not find resource please try again");
            throw new RuntimeException();
        } catch (Exception e){
            e.printStackTrace();
            log.error("Something really bad happened");
            throw new RuntimeException();
        }
        return applicationContext;
	}

    /**
     * Use reflection to get AnnotationApplicationConfigAppplicationContext and initialize using scan method
     * @param virtualFile
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
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


    /**
     * Use the NamedApplicationContext as a wrapper to the Application Context and set its name
     * @param propsParser
     * @param virtualFile
     * @param applicationContext
     * @param name
     * @return
     */
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

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
