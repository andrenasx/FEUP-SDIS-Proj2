package peer;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.Message;
import messages.protocol.BackupMessage;
import messages.protocol.ErrorMessage;
import messages.protocol.OkMessage;
import peer.storage.PeerStorage;
import peer.storage.StorageFile;
import sslengine.SSLEngineClient;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Peer extends ChordNode implements PeerInit {
    private final String serviceAccessPoint;
    private PeerStorage peerStorage;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(20);

    public Peer(String serviceAccessPoint, InetSocketAddress socketAddress, InetSocketAddress bootScoketAddress, boolean boot) throws Exception {
        super(socketAddress, bootScoketAddress, boot);
        this.serviceAccessPoint = serviceAccessPoint;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: java Peer <serviceAccessPoint> <bootAddress> <bootPort> [-b]");
            return;
        }

        String serviceAccessPoint = args[0];

        /*InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);*/
        InetSocketAddress bootSocketAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

        boolean boot = (args.length > 3 && args[3].equals("-b"));

        Peer peer;
        //Creating peer
        try {
            InetSocketAddress socketAddress;
            if (boot) {
                socketAddress = bootSocketAddress;
            } else {
                socketAddress = new InetSocketAddress("127.0.0.1", 0);
            }

            peer = new Peer(serviceAccessPoint, socketAddress, bootSocketAddress, boot);
            peer.initiate();
        } catch (Exception e) {
            System.err.println("[PEER] Error creating Peer");
            e.printStackTrace();
            return;
        }

        // Start RMI
        try {
            PeerInit stub = (PeerInit) UnicastRemoteObject.exportObject(peer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(peer.serviceAccessPoint, stub);
        } catch (RemoteException e) {
            System.err.println("[PEER] Error starting RMI");
        }

        /*PrintStream fileStream = new PrintStream(serviceAccessPoint + ".txt");
        System.setOut(fileStream);
        System.setErr(fileStream);*/
    }

    public void initiate() {
        //joins chord ring
        if (!this.join()) {
            System.err.println("[PEER] Error initializing");
            this.shutdown();
            return;
        }

        //creates storage
        this.peerStorage = new PeerStorage(this.getSelfReference().getGuid());

        //starts periodic stabilization
        this.startPeriodicStabilize();

        //starts server
        new Thread(super::start).start();

        System.out.println("[PEER] Peer inited successfully");
    }

    public void shutdown() {
        super.shutdownNode();
        super.stop();

        System.out.println("[PEER] Peer shutdown successfully");
    }


    @Override
    public void backup(String filepath, int replicationDegree) {
        File file = new File(filepath); //"../files/18kb"
        String filename = file.getName();
        BasicFileAttributes attr;
        String fileId;
        long fileSize;

        System.out.println("[PEER] Starting backup for " + filename);

        try {
            attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            fileId = Utils.generateHashForFile(filepath, attr);
            fileSize = attr.size();

            final int[] guids = new Random().ints(0, Utils.CHORD_MAX_PEERS).distinct().limit(replicationDegree * 4L).toArray();

            System.out.println("Generated GUIDS: " + Arrays.toString(guids));

            List<ChordNodeReference> peersToStore = new ArrayList<>();
            for (int guid : guids) {
                //System.out.println("Searching successor for: " + guid);
                ChordNodeReference peer = findSuccessor(guid);
                if (peer.getGuid() != this.getSelfReference().getGuid() && !peersToStore.contains(peer)) {
                    peersToStore.add(peer);
                    //System.out.println("Adding peer to store: " + peer);
                    if (peersToStore.size() == replicationDegree) break;
                }
            }

            if (peersToStore.size() == 0) {
                System.out.println("No available peers to backup file.");
                return;
            }

            StringBuilder sb = new StringBuilder("Storing GUIDS: ");
            for (ChordNodeReference node : peersToStore) {
                sb.append(node.getGuid()).append("; ");
            }
            System.out.println(sb);

            BackupMessage bm = new BackupMessage(this.getSelfReference(), (fileId + " " + filename + " " + attr.size() + " " + replicationDegree).getBytes(StandardCharsets.UTF_8));
            StorageFile storageFile = new StorageFile(this.getSelfReference(), fileId, filename, fileSize, replicationDegree);
            List<Future<String>> backups = new ArrayList<>();

            for (ChordNodeReference peer : peersToStore) {
                backups.add(this.scheduler.submit(() -> this.backupFile(bm, peer, file, storageFile)));
            }

            StringBuilder sbackups = new StringBuilder("Result for " + filename);
            //Wait for backups to finish and print result
            for (Future<String> backup : backups) {
                sbackups.append(backup.get()).append("\n");
            }

            this.peerStorage.addSentFile(storageFile);

            System.out.println(sbackups);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restore(String filepath) throws RemoteException {

    }

    @Override
    public void delete(String filepath) throws RemoteException {

    }

    @Override
    public void reclaim(double diskspace) throws RemoteException {

    }

    @Override
    public String state() throws RemoteException {
        return this.chordState();
    }


    public String backupFile(BackupMessage message, ChordNodeReference storingPeer, File file, StorageFile storageFile) {
        System.out.println("[BACKUP] Submited backup on file " + file.getName() + " for peer " + storingPeer.getGuid());
        try {
            SSLEngineClient client = new SSLEngineClient(this.getContext(), storingPeer.getSocketAddress());
            client.connect();

            Message serverreply;

            client.write(message.encode());
            //System.out.println("Sent start backup message to peer");

            serverreply = Message.create(client.read(200));
            if (serverreply instanceof OkMessage) {
                // continue
            }
            else {
                client.shutdown();

                if(serverreply instanceof ErrorMessage) {
                    client.shutdown();


                    if (((ErrorMessage) serverreply).getError().equals("NOSPACE")) {
                        return "Peer " + storingPeer.getGuid() + " does not have enough space to store the file";
                    }
                    else if (((ErrorMessage) serverreply).getError().equals("HAVEFILE")) {
                        return "Peer " + storingPeer.getGuid() + " already has requested file backed up";
                    }

                }
                else {
                    return "Received unexpected reply from Peer " + storingPeer.getGuid();
                }
            }

            //System.out.println("Received start backup response from peer");

            FileChannel fileChannel = FileChannel.open(file.toPath());
            //System.out.println("Sending file to Peer...");
            client.sendFile(fileChannel);
            //System.out.println("File sent to Peer...");

            client.read(200);

            storageFile.addStoringNode(storingPeer);
            client.shutdown();
            return "Successful backup on file " + file.getName();
        } catch (Exception e) {
            System.err.println("Couldn't backup file");
            e.printStackTrace();
            return "Failed backup on file " + file.getName();
        }
    }

    public PeerStorage getPeerStorage() {
        return peerStorage;
    }
}
