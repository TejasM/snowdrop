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
package org.jboss.spring.deployers;

import org.jboss.deployers.spi.deployer.helpers.DeploymentVisitor;
import org.jboss.spring.factory.NamedXmlApplicationContext;
import org.jboss.spring.util.JndiParse;
import org.jboss.spring.util.XmlJndiParse;
import org.jboss.spring.vfs.VFSResource;
import org.jboss.spring.vfs.context.VFSClassPathXmlApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring application context deployer.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class ApplicationContextDeployer extends AbstractSpringMetaDataDeployer<ConfigurableApplicationContext> {

    protected DeploymentVisitor<SpringMetaData> createDeploymentVisitor() {
        return new SpringDeploymentVisitor() {
            protected ConfigurableApplicationContext doCreate(SpringContextDescriptor metaData) {
            	VFSClassPathXmlApplicationContext applicationContext = new VFSClassPathXmlApplicationContext(new String[]{}, false);
                NamedXmlApplicationContext namedContext = new NamedXmlApplicationContext(applicationContext, metaData.getDefaultName());
                JndiParse parser = new XmlJndiParse();
                namedContext.initializeName(parser.getJndiName(new VFSResource(metaData.getResource())));
                return applicationContext;
            }

            protected void doClose(ConfigurableApplicationContext beanFactory) {
                beanFactory.close();
            }
        };
    }

    protected Class<ConfigurableApplicationContext> getExactBeanFactoryClass() {
        return ConfigurableApplicationContext.class;
    }
}
