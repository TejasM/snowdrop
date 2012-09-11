package org.jboss.spring.jandex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.spring.integration.test.annotation.SpringConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.jboss.spring.sampleclasses.SimpleClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

@RunWith(Arquillian.class)
@SpringConfiguration
public class JandexScanningTest {

    @Deployment
    public static WebArchive createTestArchive() {
    	WebArchive archive = ShrinkWrap.create(WebArchive.class, "spring-test.war")
                .addPackage(SimpleClass.class.getPackage())
                .addAsResource("applicationContext.xml")
                .addAsWebInfResource("web.xml");              
               //.addAsLibraries(springDependencies());
    	return archive;
    }
    
    public static File[] springDependencies() {
        ArrayList<File> files = new ArrayList<File>();
        files.addAll(resolveDependencies("org.springframework:spring-context:3.1.1.RELEASE"));
        files.addAll(resolveDependencies("org.springframework:spring-beans:3.1.1.RELEASE"));
        files.addAll(resolveDependencies("org.springframework:spring-core:3.1.1.RELEASE"));
        return files.toArray(new File[files.size()]);
    }
    
    public static List<File> resolveDependencies(String artifactName) {
        MavenDependencyResolver mvnResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        mvnResolver.loadMetadataFromPom("pom.xml");
        return Arrays.asList(mvnResolver.artifacts(artifactName).resolveAsFiles());
    }
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    public void testEcho(){
    	Assert.assertNotNull(context);
    	try {
    		Resource[] resources = ((ClassPathXmlApplicationContext)context).getResources(".*");
    		for (int i = 0; i < resources.length; i++) {
    			System.out.println("Context Loading from " + ((ClassPathXmlApplicationContext)context).getResources("**")[i].getFilename());
			}
    		System.out.println(((ClassPathXmlApplicationContext)context).getBeanFactory());
    		System.out.println(((ClassPathXmlApplicationContext)context).getAutowireCapableBeanFactory());
    		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Assert.assertEquals(106, context.getBeanDefinitionCount());
    }
}
