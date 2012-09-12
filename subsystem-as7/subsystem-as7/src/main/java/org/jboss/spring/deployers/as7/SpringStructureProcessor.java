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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;

/**
 * @author Marius Bogoevici
 */
public class SpringStructureProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.snowdrop");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        
        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        if (deploymentRoot == null) {
            return;
        }
        
    	Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(phaseContext.getDeploymentUnit());
    	for(ResourceRoot root: indexes.keySet()){
    		if(root.getRootName().equals("classes")){
    			SpringDeployment.index = indexes.get(root);
    		}
    	}

        Set<VirtualFile> springContextLocations = new HashSet<VirtualFile>();
        VirtualFile metaInf = deploymentRoot.getRoot().getChild("META-INF");
        for (VirtualFile child : metaInf.getChildren()) {
            if (child.getName().endsWith("-spring.xml")) {
                springContextLocations.add(child);
                log.debug("Found:" + child.getPathName());
            }
            if (child.getName().endsWith("-spring.properties")) {
                springContextLocations.add(child);
                log.debug("Found:" + child.getPathName());
            }
        }
        
        metaInf = deploymentRoot.getRoot().getChild("WEB-INF/classes/META-INF");
		if (metaInf != null) {
			for (VirtualFile child : metaInf.getChildren()) {
				if (child.getName().endsWith("-spring.xml")) {
					springContextLocations.add(child);
					log.debug("Found:" + child.getPathName());
				}
				if (child.getName().endsWith("-spring.properties")) {
					springContextLocations.add(child);
					log.debug("Found:" + child.getPathName());
				}
			}
		}
        
		if (!springContextLocations.isEmpty()) {
			String xmlApplicationContext = SpringDeployment.xmlApplicationContext;
			SpringDeployment springDeployment = new SpringDeployment(
					springContextLocations);
			SpringDeployment.xmlApplicationContext=xmlApplicationContext;
			springDeployment.attachTo(deploymentUnit);
			try {
				Class.forName("org.springframework.context.annotation.AnnotationConfigApplicationContext");
				springDeployment.setSpringVersion("3.0+");
			} catch (Exception e) {
				try {
					Class.forName("org.springframework.context.support.AbstractXmlApplicationContext");
				} catch (ClassNotFoundException e1) {
					// TODO: what to do if neither is installed (only warn or
					// give error?), giving warning for now.
					System.out
							.println("Snowdrop detected no spring module, make sure you have installed spring dependencies correctly");
					return;
				}
				springDeployment.setSpringVersion("2.5");

			}
		}

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
