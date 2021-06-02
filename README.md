# SDIS Project

SDIS Project for group T4G22.

Group members:

1. André Nascimento (up201806461@edu.up.pt)
2. Gustavo Sena (up201806078@edu.up.pt)
3. Luís Recharte (up201806743@edu.up.pt)
4. Rodrigo Reis (up201806534@edu.up.pt)


## Compiling
- To compile all the java code go to the `src` folder, open a terminal and run `sh ../scripts/compile.sh`
- After that a `build` folder with all .class files will be created inside that ``src`` folder

## Running
Inside the `src/build` folder:

1. Open a terminal and run `rmiregistry` to start the RMI needed for the TestApp
2. Open as many terminals needed for the number of peers and run `sh ../../scripts/peer.sh <svc_access_point> <boot_address> <boot_port> -b` to create the chord ring's boot Peer 
   or run `sh ../../scripts/peer.sh <svc_access_point> <boot_address> <boot_port> <peer_address> <peer_port>` to create a normal Peer to join the ring
    - svc_access_point: Access point for RMI object.
    - boot_address: The address of the boot Peer
    - boot_port: The port of the boot Peer
    - peer_address: The address of the Peer to join the ring
    - peer_port: The port of the Peer to join the ring
   
3. Open a terminal for the TestApp and run `sh ../../scripts/test.sh <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE|SHUTDOWN [<opnd_1> [<optnd_2]]`
    - peer_ap: Is the peer's access point for RMI object.
    - opnd_1: Is either the path name of the file to BACKUP/RESTORE/DELETE, for the respective 3 subprotocols, or, in the case of RECLAIM the maximum amount of disk space (in Byte) that the service can use to store the chunks.
    - opnd_2: This operand is an integer that specifies the desired replication degree and applies only to the BACKUP protocol
   
4. Finally, if you want to clean the created PeerStorage, open a new terminal and run `sh ../../scripts/cleanup.sh`

### Testing Example
1. Open 1 terminal (TERMINAL1) in `src` folder
2. In TERMINAL1: `sh ../scripts/compile.sh`
3. Open 12 terminals (TERMINAL2, TERMINAL3, TERMINAL4, TERMINAL5, TERMINAL6, TERMINAL7, TERMINAL8, TERMINAL9, TERMINAL10, TERMINAL11, TERMINAL12) inside `src/build` folder
4. In TERMINAL2: `rmiregistry`
5. In TERMINAL3: `sh ../scripts/peer.sh peer1 127.0.0.1 8001 -b`
6. In TERMINAL4: `sh ../scripts/peer.sh peer2 127.0.0.1 8001 127.0.0.1 8002`
7. In TERMINAL5: `sh ../scripts/peer.sh peer3 127.0.0.1 8001 127.0.0.1 8003`
8. In TERMINAL6: `sh ../scripts/peer.sh peer4 127.0.0.1 8001 127.0.0.1 8004`
9. In TERMINAL7: `sh ../scripts/peer.sh peer5 127.0.0.1 8001 127.0.0.1 8005`
10. In TERMINAL8: `sh ../scripts/peer.sh peer6 127.0.0.1 8001 127.0.0.1 8006`
11. In TERMINAL9: `sh ../scripts/peer.sh peer7 127.0.0.1 8001 127.0.0.1 8007`
12. In TERMINAL10: `sh ../scripts/peer.sh peer8 127.0.0.1 8001 127.0.0.1 8008`
13. In TERMINAL11: `sh ../scripts/peer.sh peer9 127.0.0.1 8001 127.0.0.1 8009`
14. In TERMINAL12:
    - `sh ../scripts/test.sh peer4 BACKUP ../files/1mb.jpg 3`
    - `sh ../scripts/test.sh peer4 STATE`
    - `sh ../scripts/test.sh peer4 RESTORE ../files/1mb.jpg`
    - `sh ../scripts/test.sh peer2 STATE`
    - `sh ../scripts/test.sh peer2 RECLAIM 10000`
    - `sh ../scripts/test.sh peer2 STATE`
    - `sh ../scripts/test.sh peer4 DELETE ../files/1mb.jpg`
    - `sh ../scripts/test.sh peer2 SHUTDOWN`
    - `sh ../scripts/cleanup.sh`
