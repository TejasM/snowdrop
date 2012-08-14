mvn clean package
if [ $? != 0 ]; then
{
    echo "Maven Error"
    exit 1
} fi
rm -rf $JBOSS_HOME/modules/org/jboss/snowdrop/main/*.jar
rm -rf $JBOSS_HOME/modules/org/jboss/snowdrop/main/*.index
cp deployers/deployers-aggregator/target/snowdrop-deployers.jar $JBOSS_HOME/modules/org/jboss/snowdrop/main/snowdrop-deployers.jar
cp subsystem-as7/subsystem-as7/target/snowdrop-subsystem-as7.jar $JBOSS_HOME/modules/org/jboss/snowdrop/main/snowdrop-subsystem-as7.jar
cp vfs/target/snowdrop-vfs.jar $JBOSS_HOME/modules/org/jboss/snowdrop/main/snowdrop-vfs.jar
cp subsystem-as7/subsystem-as7/target/dependency/lib/snowdrop-deployers-jandex.jar $JBOSS_HOME/modules/org/jboss/snowdrop/main/snowdrop-deployers-jandex.jar
cp subsystem-as7/subsystem-as7/target/dependency/lib/snowdrop-vfs-jandex.jar $JBOSS_HOME/modules/org/jboss/snowdrop/main/snowdrop-vfs-jandex.jar
