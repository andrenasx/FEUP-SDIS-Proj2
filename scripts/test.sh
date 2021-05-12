argc=$#

if (( argc != 5 ))
then
	echo "Usage: $0 <peer_ap> SEND <remote_addr> <port> <message>"
	exit 1
fi

# Assign input arguments to nicely named variables

pap=$1
oper=$2
host=$3
port=$4
msg=$5

echo ${pap} ${oper} ${host} ${port} ${msg}

java test.TestApp ${pap} ${oper} ${host} ${port} ${msg}