sh ./scripts/compile.sh

cd build

killall rmiregistry

sleep 2

rmiregistry &

gnome-terminal -- sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer2 127.0.0.1 8001 127.0.0.1 8002

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer3 127.0.0.1 8001 127.0.0.1 8003

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer4 127.0.0.1 8001 127.0.0.1 8004

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer5 127.0.0.1 8001 127.0.0.1 8005

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer6 127.0.0.1 8001 127.0.0.1 8006

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer7 127.0.0.1 8001 127.0.0.1 8007


