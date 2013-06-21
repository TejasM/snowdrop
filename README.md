Snowdrop
==================

WHAT IS IT?
-----------

Snowdrop is a utility package that contains JBoss-specific extensions to the Spring Framework.

PURPOSE
-------

Snowdrop provides:

A utilities library containing extensions to Spring Framework classes, that can be used wherever the generic implementations provided by the Spring Framework do not integrate correctly with the JBoss Application Server, or for integrating directly with the underlying JBoss Microcontainer
JBoss AS-specific components for deploying and running Spring applications (the Spring Deployer, i.e. *.spring)

For more details check: <https://access.redhat.com/site/documentation/en-US/JBoss_Web_Framework_Kit/2.2/html-single/Snowdrop_User_Guide/index.html>.


MANUAL INSTALLATION
-------------------

First build Snowdrop using: mvn clean package

Note: If you want to install to your local maven repository run mvn clean install.

For JBoss 7+/EAP 6+:

Open subsystem-as7/subsystem-as7/target/jboss-spring-deployer-as7.zip. Extract it to $JBOSS_HOME/modules/ if you are using JBoss 7-7.1.x or EAP 6.0.x, or to $JBOSS_HOME/modules/system/layers/base/ if otherwise.

Next, open subsystem-as-7/modules/spring-x/target/spring-2.5-module-with-dep.zip. Extract it to $JBOSS_HOME/modules/ if you are using JBoss 7-7.1.x or EAP 6.0.x, or to $JBOSS_HOME/modules/system/layers/base/ if otherwise.

Lastly open up $JBOSS_HOME/standalone/configuration/standalone.xml. Add the following:

```xml
<extensions>
      ...
      <extension module="org.jboss.snowdrop"/>
</extensions>

<profile>
      ...
      <subsystem xmlns="urn:jboss:domain:snowdrop:1.0"/>
</profile>
```

For JBoss 5/6/EAP 5:

Coming Soon!


AUTOMATIC INSTALLATION
----------------------

This process copies the necessary snowdrop and spring jars in their proper location within ${JBOSS_HOME}/modules.

It also modifies the standalone.xml and registers the snowdrop extension and subsystem, removing the need for manual installation.

Simply, run:

		mvn install -DJBOSS_HOME=/path/to/jboss_home

By default spring 3.2.2.RELEASE will be installed. To change this simply execute:

		mvn install -P${desired-spring-version} -DJBOSS_HOME=/path/to/jboss_home

There are four possible spring version profiles: spring-2.5, spring-3, spring-3.1, and spring-3.2 (the default).

_NOTE: running just mvn install by itself will only do standard maven install's lifecycle process, and not install to your JBOSS_HOME, since it doesn't know where it is._

Congratulations you have just install Snowdrop!


