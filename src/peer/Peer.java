package peer;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.Message;
import messages.protocol.*;
import storage.StorageFile;
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
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(17);

    public Peer(String serviceAccessPoint, InetSocketAddress socketAddress, InetSocketAddress bootSocketAddress, boolean boot) throws Exception {
        super(socketAddress, bootSocketAddress, boot);
        this.serviceAccessPoint = serviceAccessPoint;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Peer <serviceAccessPoint> <bootAddress> <bootPort> [-b]");
            return;
        }

        String serviceAccessPoint = args[0];
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
    }

    public void initiate() {
        // Joins chord ring
        if (!this.join()) {
            System.err.println("[PEER] Error initializing");
            this.shutdown();
            return;
        }

        // Starts periodic stabilization
        this.startPeriodicStabilize();

        // Start thread to save peer state every minute
        this.scheduler.scheduleAtFixedRate(new Thread(this.getNodeStorage()::saveState), 1, 1, TimeUnit.MINUTES);

        System.out.println("[PEER] Peer inited successfully");
    }

    public void shutdownPeer() {
        super.shutdownNode();
        this.scheduler.shutdown();
        System.out.println("[PEER] Peer shutdown successfully");
        System.exit(0);
    }

    @Override
    public void backup(String filepath, int replicationDegree) {
        // Verify if file is already backed up
        if (this.getNodeStorage().getSentFile(filepath) != null) {
            System.out.println("[ERROR-BACKUP] you have already backed up this file " + filepath);
            return;
        }

        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(Paths.get(filepath), BasicFileAttributes.class);
        } catch (IOException e) {
            System.err.println("[ERROR-BACKUP] Couldn't read file attributes. " + e.getMessage());
            return;
        }

        System.out.println("[BACKUP] Starting backup for " + filepath);

        // Generate random keys, used to store the file
        final int[] keys = new Random().ints(0, Utils.CHORD_MAX_PEERS).distinct().limit(replicationDegree * 4L).toArray();

        System.out.println("[BACKUP] Generated Keys: " + Arrays.toString(keys));

        // Find successors of the given keys, so we have the nodes that will store the files
        Map<ChordNodeReference, Integer> nodesToStore = new HashMap<>();
        for (int key : keys) {
            ChordNodeReference node = this.findSuccessor(key);
            if (node.getGuid() != this.getSelfReference().getGuid() && !nodesToStore.containsKey(node)) {
                nodesToStore.put(node, key);
                if (nodesToStore.size() == replicationDegree) break;
            }
        }

        if (nodesToStore.isEmpty()) {
            System.out.println("[ERROR-BACKUP] No available peers to backup file.");
            return;
        }

        StringBuilder sb = new StringBuilder("[BACKUP] Storing Keys: ");
        for (Integer key : nodesToStore.values()) {
            sb.append(key).append("; ");
        }
        System.out.println(sb);

        // Generate unique file id
        String fileId = Utils.generateHashForFile(filepath, attr);
        double fileSize = attr.size();

        // Create StorageFile instance with file information
        StorageFile sentFile = new StorageFile(-1, this.getSelfReference(), fileId, filepath, fileSize, replicationDegree);

        // Read file data
        byte[] fileData;
        try {
            fileData = Files.readAllBytes(Paths.get(filepath));
        } catch (IOException e) {
            System.err.println("[ERROR-BACKUP] Aborting backup, couldn't read " + filepath + " data. " + e.getMessage());
            return;
        }

        // Submit backup file workers for each node
        List<Future<String>> backups = new ArrayList<>();
        for (Map.Entry<ChordNodeReference, Integer> entry : nodesToStore.entrySet()) {
            BackupMessage bm = new BackupMessage(this.getSelfReference(), new StorageFile(entry.getValue(), this.getSelfReference(), fileId, filepath, fileSize, replicationDegree), fileData);
            backups.add(this.scheduler.submit(() -> this.backupFile(bm, entry.getKey(), sentFile)));
        }

        // Wait for backups to finish and print result
        try {
            StringBuilder sbackups = new StringBuilder("[BACKUP] Result for file " + filepath + ", with id=" + fileId + "\n");
            for (Future<String> backup : backups) {
                sbackups.append(backup.get()).append("\n");
            }
            this.getNodeStorage().addSentFile(sentFile);

            System.out.println(sbackups);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void restore(String filepath) throws RemoteException {
        System.out.println("[PEER] Starting restore for " + filepath);

        StorageFile storageFile = this.getNodeStorage().getSentFile(filepath);
        if (storageFile == null) {
            System.err.println("[RESTORE-ERROR] Did not backup " + filepath);
            return;
        }

        // Send a getmessage to each of the storing keys until we restore file, otherwise fails
        GetFileMessage rm = new GetFileMessage(this.getSelfReference(), storageFile.getFileId());
        for (Integer key : storageFile.getStoringKeys()) {
            ChordNodeReference node = this.findSuccessor(key);

            System.out.println("[RESTORE] Restoring from " + node);

            File file = new File(filepath);
            String restoreFilePath = file.getParent() + "/restored_" + file.getName();
            try {
                Files.createDirectories(Paths.get(file.getParent()));
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            if (this.restoreFile(rm, node, restoreFilePath)) {
                System.out.println("[RESTORE] Successful restore on file " + filepath);
                return;
            }
        }

        System.err.println("[ERROR-RESTORE] Failed restore on " + filepath);
    }

    @Override
    public void delete(String filepath) throws RemoteException {
        // Verify if file exists in sent files
        StorageFile sentFile = this.getNodeStorage().getSentFile(filepath);
        if (sentFile == null) {
            System.err.println("[ERROR-DELETE] Did not backup " + filepath);
            return;
        }

        Set<Integer> storingKeys = sentFile.getStoringKeys();

        // Find nodes that are storing this file keys
        List<ChordNodeReference> storingNodes = new ArrayList<>();
        for (int key : storingKeys) {
            ChordNodeReference peer = this.findSuccessor(key);
            storingNodes.add(peer);
        }


        // Submit delete worker for each storing node
        List<Future<String>> deletes = new ArrayList<>();
        for (ChordNodeReference guid : storingNodes) {
            DeleteMessage deleteMessage = new DeleteMessage(this.getSelfReference(), sentFile.getFileId());
            deletes.add(this.scheduler.submit(() -> this.deleteFile(deleteMessage, guid, sentFile)));
        }

        // Wait for deletes to finish and print result
        try {
            StringBuilder sdeletes = new StringBuilder("[DELETE] Result for " + filepath + "\n");
            for (Future<String> delete : deletes) {
                sdeletes.append(delete.get()).append("\n");
            }
            System.out.println(sdeletes);

            if (sentFile.getStoringKeys().isEmpty())
                this.getNodeStorage().removeSentFile(filepath);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reclaim(double diskspace) throws RemoteException {
        // Set new storage capacity
        this.getNodeStorage().setStorageCapacity(diskspace);

        // Try to delete files until occupied space is below desired capacity
        for (Map.Entry<String, StorageFile> entry : this.getNodeStorage().getStoredFiles().entrySet()) {
            StorageFile storedFile = entry.getValue();

            String fileId = storedFile.getFileId();
            int fileKey = storedFile.getKey();

            try {
                if (Files.deleteIfExists(Paths.get(this.getNodeStorage().getStoragePath() + fileId))) {
                    this.getNodeStorage().removeStoredFile(fileId);

                    RemovedMessage removedMessage = new RemovedMessage(this.getSelfReference(), fileId, fileKey);
                    this.sendClientMessage(storedFile.getOwner().getSocketAddress(), removedMessage);

                    System.out.println("[RECLAIM] Deleted file. FileId=" + fileId);
                }
            } catch (IOException e) {
                System.err.println("[ERROR-RECLAIM] Unable to delete file and send message. FileId=" + storedFile.getFileId());
            }

            if (this.getNodeStorage().getOccupiedSpace() <= diskspace) break;
        }

        System.out.println("[RECLAIM] Finished. New capacity: " + this.getNodeStorage().getStorageCapacity());
    }

    @Override
    public String state() throws RemoteException {
        return "\nChord State\n" + this.chordState() + "\nSTORAGE\n" + this.getNodeStorage();
    }

    @Override
    public void shutdown() {
        super.shutdownSafely();
        this.shutdownPeer();
    }

    public String backupFile(BackupMessage message, ChordNodeReference targetNode, StorageFile storageFile) {
        // Send backup message and process the response
        try {
            Message response = this.sendAndReceiveMessage(targetNode.getSocketAddress(), message);

            if (response instanceof OkMessage) {
                // Backup successful, add this file key to our sent file storing keys
                storageFile.addStoringKey(message.getStorageFile().getKey());
                return "[BACKUP] Successful backup on file " + storageFile.getFilePath() + " for peer " + targetNode.getGuid();
            }
            else if (response instanceof ErrorMessage) {
                if (((ErrorMessage) response).getError().equals("FULL")) {
                    // Peer doesn't have space to store this file
                    return "[ERROR-BACKUP] Peer " + targetNode.getGuid() + " does not have enough space to store the file";
                }
                else if (((ErrorMessage) response).getError().equals("HAVE")) {
                    // Peer already had this file backed up
                    return "[ERROR-BACKUP] Peer " + targetNode.getGuid() + " already has requested file backed up";
                }

                return "[ERROR-BACKUP] Received unexpected error from Peer " + targetNode.getGuid();
            }
            else {
                return "[ERROR-BACKUP] Received unexpected reply from Peer " + targetNode.getGuid();
            }
        } catch (Exception e) {
            System.err.println("[ERROR-BACKUP] Couldn't backup file " + storageFile.getFilePath());
            return "[ERROR-BACKUP] Failed backup on file " + storageFile.getFilePath() + " on peer " + targetNode.getGuid();
        }
    }

    public boolean restoreFile(GetFileMessage message, ChordNodeReference targetNode, String restoreFilePath) {
        // Send restore message and process the response
        try {
            Message response = this.sendAndReceiveMessage(targetNode.getSocketAddress(), message);

            if (response instanceof FileMessage) {
                // Received file message, save file
                Files.write(Paths.get(restoreFilePath), ((FileMessage) response).getFileData());
                return true;
            }
            else if (response instanceof ErrorMessage) {
                System.out.println("[ERROR-RESTORE] Received error from Peer " + targetNode.getGuid());
            }
            else {
                System.out.println("[ERROR-RESTORE] Received unexpected reply from Peer " + targetNode.getGuid());
            }
        } catch (Exception e) {
            System.err.println("[ERROR-RESTORE] Couldn't restore file");
            e.printStackTrace();
        }

        return false;
    }

    public String deleteFile(DeleteMessage message, ChordNodeReference targetNode, StorageFile storageFile) {
        return this.deleteFile(message, targetNode, storageFile, false);
    }

    public String deleteFile(DeleteMessage message, ChordNodeReference storingNode, StorageFile targetNode, boolean chord) {
        // Send delete message and process the response
        try {
            Message response = this.sendAndReceiveMessage(storingNode.getSocketAddress(), message);

            if (response instanceof OkMessage) {
                // if it is a delete done by protocol remove storing key from our sent file
                if (!chord) targetNode.removeStoringKey(Integer.parseInt(((OkMessage) response).getBody()));

                return "[DELETE] Successful delete file " + targetNode.getFilePath() + " for peer " + storingNode.getGuid();
            }
            else if (response instanceof ErrorMessage) {
                return "[ERROR-DELETE] Peer " + storingNode.getGuid() + " failed to delete file";
            }
            else {
                return "[ERROR-DELETE] Received unexpected reply from Peer " + storingNode.getGuid();
            }
        } catch (Exception e) {
            System.err.println("[ERROR-DELETE] Couldn't delete file " + targetNode.getFilePath());
            return "[ERROR-DELETE] Failed delete on file " + targetNode.getFilePath() + " on peer " + storingNode.getGuid();
        }
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
