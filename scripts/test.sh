argc=$#

if [ $argc -lt 2 ]
then
	echo "Usage: $0 <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE|SHUTDOWN [<opnd_1> [<optnd_2]]"
	exit 1
fi

# Assign input arguments to nicely named variables

pap=$1
oper=$2

# Validate remaining arguments

case $oper in
BACKUP)
	if [ $argc -ne 4 ]
	then
		echo "Usage: $0 <peer_ap> BACKUP <filename> <rep degree>"
		exit 1
	fi
	opernd_1=$3
	rep_deg=$4
	;;
RESTORE)
	if [ $argc -ne 3 ]
	then
		echo "Usage: $0 <peer_app> RESTORE <filename>"
	fi
	opernd_1=$3
	rep_deg=""
	;;
DELETE)
	if [ $argc -ne 3 ]
	then
		echo "Usage: $0 <peer_app> DELETE <filename>"
		exit 1
	fi
	opernd_1=$3
	rep_deg=""
	;;
RECLAIM)
	if [ $argc -ne 3 ]
	then
		echo "Usage: $0 <peer_app> RECLAIM <max space>"
		exit 1
	fi
	opernd_1=$3
	rep_deg=""
	;;
STATE)
	if [ $argc -ne 2 ]
	then
		echo "Usage: $0 <peer_app> STATE"
		exit 1
	fi
	opernd_1=""
	rep_deg=""
	;;
SHUTDOWN)
	if [ $argc -ne 2 ]
	then
		echo "Usage: $0 <peer_app> SHUTDOWN"
		exit 1
	fi
	opernd_1=""
	rep_deg=""
	;;
*)
	echo "Usage: $0 <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE|SHUTDOWN [<opnd_1> [<optnd_2]]"
	exit 1
	;;
esac


java test.TestApp ${pap} ${oper} ${opernd_1} ${rep_deg}
