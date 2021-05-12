sh ./scripts/compile.sh

cd build

start rmiregistry

start cmd /k sh ../scripts/peer.sh peer1 localhost 9222
start cmd /k sh ../scripts/peer.sh peer2 localhost 9223
start cmd /k sh ../scripts/peer.sh peer2 localhost 9224
