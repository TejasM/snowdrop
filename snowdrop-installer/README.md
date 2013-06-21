Snowdrop Installer
==================

Purpose
--------

To enable ease of installation of the snowdrop module in the standalone version of JBOSS AS/EAP.

What-it-does
------------

It copies the necessary snowdrop and spring jars in their proper location within ${JBOSS_HOME}/modules.

It also modifies the standalone.xml and registers the snowdrop extension and subsystem, removing the need for manual installation.

How-to-use
-----------

_NOTE: Make sure to pass in -DJBOSS_HOME=/path/to/either your JBoss or EAP installation_

Simply, run:

		mvn install -DJBOSS_HOME=/path/to/jboss_home

By default spring 3.2.2.RELEASE will be installed. To change this simply execute:

		mvn install -P${desired-spring-version} -DJBOSS_HOME=/path/to/jboss_home

There are four possible spring version profiles: spring-2.5, spring-3, spring-3.1, and spring-3.2 (the default).
