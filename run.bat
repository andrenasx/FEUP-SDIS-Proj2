sh ./scripts/compile.sh

cd build

start rmiregistry

start cmd /k sh ../scripts/peer.sh peer1 localhost 8001 -b

timeout 5

start cmd /k sh ../scripts/peer.sh peer2 localhost 8001

timeout 5

start cmd /k sh ../scripts/peer.sh peer3 localhost 8001
