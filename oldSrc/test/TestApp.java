package test;

import peer.PeerInit;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        String serviceAccessPoint = args[0];
        String protocol = args[1].toUpperCase();
        PeerInit peer;

        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            peer = (PeerInit) registry.lookup(serviceAccessPoint);

            // Parse arguments according to protocol
            switch (protocol) {
                case "BACKUP": {
                    if (args.length != 4) {
                        System.out.println("Usage: java TestApp <peer_ap> BACKUP <file_path> <replication_degree>");
                        return;
                    }

                    String filepath = args[2];
                    int replicationDeg = Integer.parseInt(args[3]);
                    peer.backup(filepath, replicationDeg);

                    break;
                }
                case "RESTORE": {
                    if (args.length != 3) {
                        System.out.println("Usage: java TestApp <peer_ap> RESTORE <file_path>");
                        return;
                    }

                    String filepath = args[2];
                    peer.restore(filepath);

                    break;
                }
                case "DELETE": {
                    if (args.length != 3) {
                        System.out.println("Usage: java TestApp <peer_ap> DELETE <file_path>");
                        return;
                    }

                    String filepath = args[2];
                    peer.delete(filepath);

                    break;
                }
                case "RECLAIM": {
                    if (args.length != 3) {
                        System.out.println("Usage: java TestApp <peer_ap> RECLAIM <disk_space>");
                        return;
                    }

                    double diskspace = Double.parseDouble(args[2]);
                    peer.reclaim(diskspace);

                    break;
                }
                case "STATE": {
                    if (args.length != 2) {
                        System.out.println("Usage: java TestApp <peer_ap> STATE");
                        return;
                    }

                    System.out.println(peer.state());

                    break;
                }
                default:
                    throw new Exception("Unknown protocol");
            }
        } catch (Exception e) {
            System.err.println("TestApp exception: " + e.toString());
        }
    }
}
