argc=$#

if (( argc != 3 ))
then
	echo "Usage: $0 <svc_access_point> <host_addr> <port>"
	exit 1
fi

# Assign input arguments to nicely named variables

sap=$1
host=$2
port=$3

# Execute the program

java sslengine.SSLPeer ${sap} ${host} ${port}
