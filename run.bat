sh ./scripts/compile.sh

cd build

start rmiregistry

start cmd /k sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b

timeout 5

start cmd /k sh ../scripts/peer.sh peer2 127.0.0.1 8001

timeout 5

start cmd /k sh ../scripts/peer.sh peer3 127.0.0.1 8001
