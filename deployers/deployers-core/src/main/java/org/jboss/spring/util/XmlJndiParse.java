package org.jboss.spring.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.jboss.spring.factory.Constants;
import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;

public class XmlJndiParse {
	public static String[] getJndiName(Resource resource){
		String name = null;
		String parentName = null;
		try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return "http://www.springframework.org/schema/beans";
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return "beans";
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return Collections.singleton("beans").iterator();
                }
            });
            String expression = "/beans:beans/beans:description";
            InputSource inputSource = new InputSource(resource.getInputStream());
            String description = xPath.evaluate(expression, inputSource);
            
            if (description != null) {
                Matcher bfm = Pattern.compile(Constants.BEAN_FACTORY_ELEMENT).matcher(description);
                if (bfm.find()) {
                    name = bfm.group(1);
                }
                Matcher pbfm = Pattern.compile(Constants.PARENT_BEAN_FACTORY_ELEMENT).matcher(description);
                if (pbfm.find()) {
                    parentName = pbfm.group(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }		
		return parentName==null ? new String[]{name} : new String[]{name, parentName};
	}
}
