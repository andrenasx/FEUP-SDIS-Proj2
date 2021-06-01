sh ./scripts/compile.sh

cd build

start rmiregistry

start cmd /k sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b

timeout 5

start cmd /k sh ../scripts/peer.sh peer2 127.0.0.1 8001 127.0.0.1 8002

timeout 5

start cmd /k sh ../scripts/peer.sh peer3 127.0.0.1 8001 127.0.0.1 8003

timeout 5

start cmd /k sh ../scripts/peer.sh peer4 127.0.0.1 8001 127.0.0.1 8004

timeout 5

start cmd /k sh ../scripts/peer.sh peer5 127.0.0.1 8001 127.0.0.1 8005

timeout 5

start cmd /k sh ../scripts/peer.sh peer6 127.0.0.1 8001 127.0.0.1 8006

timeout 5

start cmd /k sh ../scripts/peer.sh peer7 127.0.0.1 8001 127.0.0.1 8007

