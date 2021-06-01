package peer;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.Message;
import messages.protocol.*;
import peer.storage.PeerStorage;
import peer.storage.StorageFile;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

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
            }
            else {
                socketAddress = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
            }

            peer = new Peer(serviceAccessPoint, socketAddress, bootSocketAddress, boot);
        } catch (Exception e) {
            System.err.println("[PEER] Error creating Peer");
            e.printStackTrace();
            return;
        }

        peer.initiate();

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
        this.peerStorage = PeerStorage.loadState(this);

        //starts periodic stabilization
        this.startPeriodicStabilize();


        this.scheduler.scheduleAtFixedRate(new Thread(this.peerStorage::saveState), 1, 1, TimeUnit.MINUTES);
        System.out.println("[PEER] Peer inited successfully");
    }

    public void shutdown() {
        super.shutdownNode();
        try {
            super.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("[PEER] Peer shutdown successfully");
    }


    @Override
    public void backup(String filepath, int replicationDegree) {
        File file = new File(filepath); //"../files/18kb"
        BasicFileAttributes attr;
        String fileId;
        long fileSize;

        System.out.println("[BACKUP] Starting backup for " + filepath);

        try {
            attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            System.err.println("[ERROR-BACKUP] Couldn't read file attributes. " + e.getMessage());
            return;
        }
        fileId = Utils.generateHashForFile(filepath, attr);
        fileSize = attr.size();

        final int[] guids = new Random().ints(0, Utils.CHORD_MAX_PEERS).distinct().limit(replicationDegree * 4L).toArray();

        System.out.println("[BACKUP] Generated GUIDS: " + Arrays.toString(guids));

        Map<ChordNodeReference, Integer> peersToStore = new HashMap<>();
        for (int guid : guids) {
            //System.out.println("Searching successor for: " + guid);
            ChordNodeReference peer = this.findSuccessor(guid);
            if (peer.getGuid() != this.getSelfReference().getGuid() && !peersToStore.containsKey(peer)) {
                peersToStore.put(peer, guid);
                //System.out.println("Adding peer to store: " + peer);
                if (peersToStore.size() == replicationDegree) break;
            }
        }

        if (peersToStore.size() == 0) {
            System.out.println("[ERROR-BACKUP] No available peers to backup file.");
            return;
        }

        StringBuilder sb = new StringBuilder("[BACKUP] Storing GUIDS: ");
        for (ChordNodeReference node : peersToStore.keySet()) {
            sb.append(node.getGuid()).append("; ");
        }
        System.out.println(sb);

        StorageFile storageFile = new StorageFile(-1, this.getSelfReference(), fileId, filepath, fileSize, replicationDegree);
        //BackupMessage bm = new BackupMessage(this.getSelfReference(), storageFile);


        this.peerStorage.addSentFile(storageFile);

        byte[] fileData = new byte[0];
        try {
            fileData = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.err.println("[ERROR-BACKUP] Couldn't read " + filepath + " data. " + e.getMessage());
        }

        List<Future<String>> backups = new ArrayList<>();
        for (Map.Entry<ChordNodeReference, Integer> entry : peersToStore.entrySet()) {
            BackupMessage bm = new BackupMessage(this.getSelfReference(), new StorageFile(entry.getValue(), this.getSelfReference(), fileId, filepath, fileSize, replicationDegree), fileData);
            backups.add(this.scheduler.submit(() -> this.backupFile(bm, entry.getKey(), file, storageFile)));
        }

        try {
            StringBuilder sbackups = new StringBuilder("[BACKUP] Result for " + filepath + "\n");
            // Wait for backups to finish and print result
            for (Future<String> backup : backups) {
                sbackups.append(backup.get()).append("\n");
            }
            System.out.println(sbackups);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restore(String filepath) throws RemoteException {
        System.out.println("[PEER] Starting restore for " + filepath);

        StorageFile storageFile = this.peerStorage.getSentFile(filepath);

        if (storageFile == null) {
            System.err.println("[RESTORE-ERROR] Did not backup " + filepath);
            return;
        }

        RestoreMessage rm = new RestoreMessage(this.getSelfReference(), storageFile.getFileId());
        for (Integer key : storageFile.getStoringKeys()) {
            ChordNodeReference node = this.findSuccessor(key);

            System.out.println("Restoring from " + node);
            if (this.restoreFile(rm, node, filepath)) {
                System.out.println("Successfull restore on file " + filepath);
                return;
            }
        }

        System.err.println("[ERROR-RESTORE] Failed restore on " + filepath);
    }

    @Override
    public void delete(String filepath) throws RemoteException {

    }

    @Override
    public void reclaim(double diskspace) throws RemoteException {

    }

    @Override
    public String state() throws RemoteException {
        return "\nChord State\n" + this.chordState() + "\nSTORAGE\n" + this.peerStorage;

    }

    // TODO File not needed
    public String backupFile(BackupMessage message, ChordNodeReference storingPeer, File file, StorageFile storageFile) {
        //System.out.println("[BACKUP] Submitted backup on file " + file.getName() + " for peer " + storingPeer.getGuid());
        try {
            Message response = this.sendAndReceiveMessage(storingPeer.getSocketAddress(), message);

            if (response instanceof OkMessage) {
                storageFile.addStoringKey(message.getStorageFile().getKey());
                return "[BACKUP] Successful backup on file " + file.getName() + " for peer " + storingPeer.getGuid();
            }
            else if (response instanceof ErrorMessage) {
                if (((ErrorMessage) response).getError().equals("FULL")) {
                    return "[ERROR-BACKUP] Peer " + storingPeer.getGuid() + " does not have enough space to store the file";
                }
                else if (((ErrorMessage) response).getError().equals("HAVEFILE")) {
                    storageFile.addStoringKey(message.getStorageFile().getKey());
                    return "[BACKUP] Peer " + storingPeer.getGuid() + " already has requested file backed up";
                }

                return "[ERROR-BACKUP] Received unexpected error from Peer " + storingPeer.getGuid();
            }
            else {
                return "[ERROR-BACKUP] Received unexpected reply from Peer " + storingPeer.getGuid();
            }
        } catch (Exception e) {
            System.err.println("[ERROR-BACKUP] Couldn't backup file " + file.getName());
            e.printStackTrace();
            return "[ERROR-BACKUP] Failed backup on file " + file.getName() + " on peer " + storingPeer.getGuid();
        }
    }

    public boolean restoreFile(RestoreMessage message, ChordNodeReference storingPeer, String filePath) {
        try {
            Message response = this.sendAndReceiveMessage(storingPeer.getSocketAddress(), message);

            if (response instanceof FileMessage) {
                System.out.println("Restoring file...");
                File file = new File(filePath);
                String restoredFilePath = file.getParent() + "/restored_" + file.getName();
                Files.createDirectories(Paths.get(file.getParent()));

                Files.write(Paths.get(restoredFilePath), ((FileMessage) response).getFileData());

                System.out.println("[RESTORE] Successful restore on file " + filePath);
                return true;
            }
            else if (response instanceof ErrorMessage) {
                System.out.println("[ERROR-RESTORE] Received error from Peer " + storingPeer.getGuid());
            }
            else {
                System.out.println("[ERROR-RESTORE] Received unexpected reply from Peer " + storingPeer.getGuid());
            }
        } catch (Exception e) {
            System.err.println("[ERROR-RESTORE] Couldn't restore file " + filePath);
            e.printStackTrace();
        }

        return false;
    }

    public PeerStorage getPeerStorage() {
        return peerStorage;
    }
}
