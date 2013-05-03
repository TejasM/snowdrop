package org.jboss.spring.deployers.as7;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class CustomNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("component-scan", new CustomComponentScanBeanDefinition());
    }
}