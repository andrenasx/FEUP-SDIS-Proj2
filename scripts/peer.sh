argc=$#

if (( argc < 3 || argc > 4 ))
then
	echo "Usage: $0 <svc_access_point> <host_addr> <port> [-b]"
	exit 1
fi

# Assign input arguments to nicely named variables

sap=$1
host=$2
port=$3

# Execute the program

java peer.Peer ${sap} ${host} ${port} $4
