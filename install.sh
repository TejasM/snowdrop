mvn clean package
if [ $? != 0 ]; then
{
    echo "Maven Error"
    exit 1
} fi
rm -rf $JBOSS_HOME/modules/org/jboss/snowdrop/main/*
rm -rf $JBOSS_HOME/modules/org/springframework/spring/snowdrop/*
unzip subsystem-as7/subsystem-as7/target/jboss-spring-deployer-as7.zip -d /home/tmehta/jboss-as-7.1.1.Final/modules/
unzip subsystem-as7/modules/spring-3.1/target/spring-3.1-module.zip -d /home/tmehta/jboss-as-7.1.1.Final/modules/
