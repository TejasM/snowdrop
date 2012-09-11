package org.jboss.spring.sampleclasses;

import org.jboss.spring.annotations.Sample;
import org.springframework.stereotype.Component;


@Sample
public class SimpleClass {
	
	private String string;

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}
}
