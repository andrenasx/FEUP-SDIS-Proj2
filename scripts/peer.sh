argc=$#

if ((argc == 4))
then
  java peer.Peer $1 $2 $3 $4
fi
if ((argc == 5))
then
  java peer.Peer $1 $2 $3 $4 $5
fi
