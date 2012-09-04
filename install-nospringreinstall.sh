mvn clean package
if [ $? != 0 ]; then
{
    echo "Maven Error"
    exit 1
} fi
rm -rf $JBOSS_HOME/modules/org/jboss/snowdrop/main/*
unzip subsystem-as7/subsystem-as7/target/jboss-spring-deployer-as7.zip -d /home/tmehta/jboss-as-7.1.1.Final/modules/
