ipaddr=$(ifconfig  | grep 'inet addr:'| grep -v '127.0.0.1' | cut -d: -f2 | awk '{ print $1}')
$JBOSS_HOME/bin/jboss-cli.sh --controller=$ipaddr:9999 -c
if [ $? == 0 ]
then
	$JBOSS_HOME/bin/jboss-cli.sh --controller=$ipaddr:9999 -c /extension=org.jboss.snowdrop:add	
	exit 0
else
	$JBOSS_HOME/bin/jboss-cli.sh -c
	if [ $? == 0 ]
	then
		$JBOSS_HOME/bin/jboss-cli.sh -c /extension=org.jboss.snowdrop:add	
		exit 0
	else
		echo "Starting Server"
		$JBOSS_HOME/bin/domain.sh &
		PID=$!
		echo $PID
		echo "Waiting for server to start"
		sleep 1m
		echo "Activating snowdrop on server"
		$JBOSS_HOME/bin/jboss-cli.sh --controller=$ipaddr:9999 -c /extension=org.jboss.snowdrop:add
		for i in `ps -ef| awk '$3 == '$PID' { print $2 }'`
		do
			kill -9 $i
		done
		exit 0
	fi
fi