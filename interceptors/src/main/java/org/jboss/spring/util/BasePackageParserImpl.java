package org.jboss.spring.util;

import java.io.IOException;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.springframework.core.io.Resource;

public class BasePackageParserImpl implements BasePackageParser {

    private static final Logger log = Logger.getLogger("org.jboss.snowdrop");
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
		} catch (IOException e) {
            e.printStackTrace();
            log.error("Could not find resource please try again");
            throw new RuntimeException();
        }
        return basePackages;
	}

}
