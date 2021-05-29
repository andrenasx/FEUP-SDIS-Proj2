sh ./scripts/compile.sh

cd build

rmiregistry &

gnome-terminal -- sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b

sleep 3

gnome-terminal -- sh ../scripts/peer.sh peer2 127.0.0.1 8001

sleep 3

gnome-terminal -- sh ../scripts/peer.sh peer3 127.0.0.1 8001

sleep 3

gnome-terminal -- sh ../scripts/peer.sh peer4 127.0.0.1 8001

sleep 3

gnome-terminal -- sh ../scripts/peer.sh peer5 127.0.0.1 8001

sleep 3

gnome-terminal -- sh ../scripts/peer.sh peer6 127.0.0.1 8001
