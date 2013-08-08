package org.jboss.spring.util;

import java.util.Properties;

import org.springframework.core.io.Resource;

public class PropsJndiParse implements JndiParse {
	@Override
	public String[] getJndiName(Resource resource){
		String name = null;
		String parentName = null;
		try {
            Properties pros = new Properties();
            pros.load(resource.getInputStream());
            name = pros.getProperty("jndi_name");
            parentName = pros.getProperty("parent_name");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }		
		return parentName==null ? new String[]{name} : new String[]{name, parentName};
	}
}
