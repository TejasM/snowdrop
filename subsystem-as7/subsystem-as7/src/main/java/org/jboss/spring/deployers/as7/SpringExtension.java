/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import javassist.*;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Marius Bogoevici
 */
public class SpringExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.snowdrop");

    public static final String SUBSYSTEM_NAME = "spring";

    public static final String NAMESPACE = "urn:jboss:domain:snowdrop:1.0";

    private static SpringSubsystemElementParser parser = new SpringSubsystemElementParser();

    private static final DescriptionProvider SUBSYSTEM_ADD_DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return SpringDescriptionProviders.getSubsystemAddDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return SpringDescriptionProviders.getSubsystemDescription(locale);
        }
    };

    private static ModelNode createAddSubSystemOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    public void initialize(ExtensionContext context) {
        log.debug("Activating Spring Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SUBSYSTEM_DESCRIPTION);
        registration.registerOperationHandler(ADD, SpringSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESCRIPTION, false);
        registration.registerOperationHandler(DESCRIBE, SpringSubsystemDescribeHandler.INSTANCE, SpringSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(parser);

        ClassPool classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(ConfigurableApplicationContext.class));

        try {
            CtClass cc = classPool.get("org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider");

            try{
                CtField field = cc.getField("index");
                cc.removeField(field);
            }  catch(Exception e){

            } finally {
                CtField f = new CtField(classPool.get("java.lang.String"), "index", cc);
                cc.addField(f, "new String()");
            }

            try {
                cc.getDeclaredMethod("removeNonBase");
            } catch (NotFoundException e){
                CtMethod removeNonBaseMethod = new CtMethod(classPool.get("java.util.List"), "removeNonBase",
                        new CtClass[] { classPool.get("java.lang.String") }, cc);
                cc.addMethod(removeNonBaseMethod);
                removeNonBaseMethod.setBody("{String[] classesConsidered = this.index.split(\" \");" +
                        "java.util.List inBase = new java.util.ArrayList();" +
                        "for (int i =0; i < classesConsidered.length; i++){;" +
                        "if(classesConsidered[i].contains($1)){;" +
                        "inBase.add(classesConsidered[i]);};};" +
                        "return inBase;}");
            }

            try {
                cc.getDeclaredMethod("createBeanDefinitions");
            } catch (NotFoundException e) {
                CtMethod createBeansMethod = new CtNewMethod().make("private java.util.Set createBeanDefinitions(String basePackage){ " +
                        "java.util.List toConsider = removeNonBase(basePackage);" +
                        "java.util.Set beanDefs = new java.util.HashSet();" +
                        "for (int i=0;i<toConsider.size();i++){;" +
                        "String string = toConsider.get(i);" +
                        "String classPath = org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_URL_PREFIX + resolveBasePackage(string.toString()) + \".class\";" +
                        "org.springframework.core.io.support.ResourcePatternResolver resourcePatternResolver = (org.springframework.core.io.support.ResourcePatternResolver) getResourceLoader();" +
                        "try {;" +
                        "org.springframework.core.io.Resource resource = resourcePatternResolver.getResource(classPath);" +
                        "org.springframework.beans.factory.support.GenericBeanDefinition beanDefinition = new org.springframework.beans.factory.support.GenericBeanDefinition();" +
                        "beanDefinition.setResource(resource);" +
                        "beanDefinition.setBeanClass(Class.forName(string.toString(), false,  getResourceLoader().getClassLoader()));" +
                        "beanDefs.add(beanDefinition);} catch(ClassNotFoundException e){;" +
                        "return null;" +
                        "};};" +
                        "return beanDefs;}", cc);
                cc.addMethod(createBeansMethod);
            }

            CtMethod m = cc.getDeclaredMethod("findCandidateComponents");
            m.insertBefore("{ if(this.index!=null && new Exception().getStackTrace()[3].getClassName().contains(\"ComponentScanBeanDefinitionParser\")){;" +
                    "java.util.Set beanDefs = createBeanDefinitions($1);" +
                    "if(beanDefs!=null){;return beanDefs;};};}");

            cc.writeFile();
        } catch (NotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CannotCompileException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void initializeParsers(ExtensionParsingContext context) {
        log.debug("Setting up parsers");
        context.setSubsystemXmlMapping(NAMESPACE, parser);
    }

    static class SpringSubsystemElementParser implements XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            try{
				ParseUtils.nextElement(reader);
				SpringDeployment.xmlApplicationContext = reader.getElementText();				
				ParseUtils.requireNoContent(reader);
				System.out.println("Got XmlApplicationContext to be: " + SpringDeployment.xmlApplicationContext);
            } catch(Exception e) {
				System.out.println("Didn't find XmlApplicationContext Element");
            	ParseUtils.requireNoContent(reader);
            }            
            final ModelNode update = new ModelNode();
            update.get(OP).set(ADD);
            update.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
            list.add(createAddSubSystemOperation());
        }

        private static ModelNode createAddSubSystemOperation() {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
            return subsystem;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws
                XMLStreamException {
            //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
            context.startSubsystemElement(NAMESPACE, false);
            writer.writeEndElement();

        }
    }

    private static class SpringSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {

        static final SpringSubsystemDescribeHandler INSTANCE = new SpringSubsystemDescribeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getResult().add(createAddSubSystemOperation());
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }
}
