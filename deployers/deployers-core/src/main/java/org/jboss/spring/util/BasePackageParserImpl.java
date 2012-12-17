package org.jboss.spring.util;

import java.util.Properties;

import org.springframework.core.io.Resource;

public class BasePackageParserImpl implements BasePackageParser {
	
	/* (non-Javadoc)
	 * @see org.jboss.spring.util.BasePackageParser#parseBasePackages(org.springframework.core.io.Resource)
	 */
	@Override
	public String[] parseBasePackages(Resource resource) {
		String[] basePackages = new String[] {};
		try {
			Properties props = new Properties();
			props.load(resource.getInputStream());
			String packages = props.getProperty("package_list");
			basePackages = packages.split(",");
		} catch (Exception e) {
			// TODO: handle exception
		}
		return basePackages;
	}

}
