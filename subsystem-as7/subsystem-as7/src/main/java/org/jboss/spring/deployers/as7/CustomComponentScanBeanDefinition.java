package org.jboss.spring.deployers.as7;


import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.util.Set;

public class CustomComponentScanBeanDefinition extends ComponentScanBeanDefinitionParser {

    private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";

    private static final String RESOURCE_PATTERN_ATTRIBUTE = "resource-pattern";

    private static final String USE_DEFAULT_FILTERS_ATTRIBUTE = "use-default-filters";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String[] basePackages = StringUtils.tokenizeToStringArray(element.getAttribute(BASE_PACKAGE_ATTRIBUTE),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        // Actually scan for bean definitions and register them.
        CustomClassPathBeanDefinitionScanner scanner = customConfigureScanner(parserContext, element);
        Set<BeanDefinitionHolder> beanDefinitions = scanner.doScan(basePackages);
        registerComponents(parserContext.getReaderContext(), beanDefinitions, element);

        return null;
    }

    protected CustomClassPathBeanDefinitionScanner customConfigureScanner(ParserContext parserContext, Element element) {
        XmlReaderContext readerContext = parserContext.getReaderContext();

        boolean useDefaultFilters = true;
        if (element.hasAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE)) {
            useDefaultFilters = Boolean.valueOf(element.getAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE));
        }

        // Delegate bean definition registration to scanner class.
        CustomClassPathBeanDefinitionScanner scanner = customCreateScanner(readerContext, useDefaultFilters);
        scanner.setResourceLoader(readerContext.getResourceLoader());
        scanner.setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
        scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

        if (element.hasAttribute(RESOURCE_PATTERN_ATTRIBUTE)) {
            scanner.setResourcePattern(element.getAttribute(RESOURCE_PATTERN_ATTRIBUTE));
        }

        try {
            parseBeanNameGenerator(element, scanner);
        }
        catch (Exception ex) {
            readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
        }

        try {
            parseScope(element, scanner);
        }
        catch (Exception ex) {
            readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
        }

        parseTypeFilters(element, scanner, readerContext);

        return scanner;
    }

    protected CustomClassPathBeanDefinitionScanner customCreateScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        return new CustomClassPathBeanDefinitionScanner(readerContext.getRegistry(), useDefaultFilters);
    }


}
