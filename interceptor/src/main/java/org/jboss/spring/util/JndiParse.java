package org.jboss.spring.util;

import org.springframework.core.io.Resource;

public interface JndiParse {
	
	public String[] getJndiName(Resource resource);

}