package org.jboss.spring.util;

import org.springframework.core.io.Resource;

public interface BasePackageParser {

	public abstract String[] parseBasePackages(Resource resource);

}