sh ./scripts/compile.sh

cd build

rmiregistry &

gnome-terminal -- sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer2 127.0.0.1 8001 127.0.0.1 8002

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer3 127.0.0.1 8001 127.0.0.1 8003

sleep 5

gnome-terminal -- sh ../scripts/peer.sh peer4 127.0.0.1 8001 127.0.0.1 8004


