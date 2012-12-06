ipaddr=$(ifconfig  | grep 'inet addr:'| grep -v '127.0.0.1' | cut -d: -f2 | awk '{ print $1}')

$1/bin/jboss-cli.sh --controller=$ipaddr:9999 -c version
if [ $? == 0 ]
then
	$1/bin/jboss-cli.sh --controller=$ipaddr:9999 -c /extension=org.jboss.snowdrop:add	
	exit 0
else
	$1/bin/jboss-cli.sh -c version
	if [ $? == 0 ]
	then
		$1/bin/jboss-cli.sh -c /extension=org.jboss.snowdrop:add
		exit 0
	else
		echo "Starting Server"
		$1/bin/domain.sh &
		PID=$!
		echo $PID
		echo "Waiting for server to start"
		sleep 1m
		echo "Activating snowdrop on server"
		$1/bin/jboss-cli.sh --controller=$ipaddr:9999 -c /extension=org.jboss.snowdrop:add
		for i in `ps -ef| awk '$3 == '$PID' { print $2 }'`
		do
			kill -9 $i
		done
		exit 0
	fi
fi