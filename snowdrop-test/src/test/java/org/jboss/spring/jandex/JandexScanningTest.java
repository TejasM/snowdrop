package org.jboss.spring.jandex;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.spring.integration.test.annotation.SpringConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.spring.annotations.Sample;
import org.jboss.spring.sampleclasses.SimpleClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@RunWith(Arquillian.class)
@SpringConfiguration
public class JandexScanningTest {

    @Deployment
    public static WebArchive createTestArchive() {
    	WebArchive archive = ShrinkWrap.create(WebArchive.class, "spring-test.war")
                .addPackage(SimpleClass.class.getPackage())
                .addPackage(Sample.class.getPackage())
                .addAsResource("applicationContext.xml")
                .addAsWebInfResource("web.xml");              
    	return archive;
    }
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    public void testEcho(){
    	Assert.assertNotNull(context);
    	Assert.assertEquals(1, context.getBeanNamesForType(SimpleClass.class).length);
    	Assert.assertTrue(context.containsBean("simpleClass"));
    	Assert.assertEquals(106, context.getBeanDefinitionCount());
    }
}
