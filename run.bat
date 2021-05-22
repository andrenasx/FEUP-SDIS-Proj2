sh ./scripts/compile.sh

cd build

start rmiregistry

start cmd /k sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b

timeout 3

start cmd /k sh ../scripts/peer.sh peer2 127.0.0.1 8001

timeout 3

start cmd /k sh ../scripts/peer.sh peer3 127.0.0.1 8001

timeout 3

start cmd /k sh ../scripts/peer.sh peer4 127.0.0.1 8001