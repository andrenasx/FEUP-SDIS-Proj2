package peer;

import channel.MulticastChannel;
import messages.Message;
import storage.Chunk;
import storage.StorageFile;
import workers.WakeyWorker;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

public class Peer implements PeerInit {
    private final int id;
    private final String protocolVersion;
    private final String serviceAccessPoint; // remoteObjectName since we will be using RMI

    private final PeerStorage storage;

    // Multicast Channels
    private final MulticastChannel mcChannel; // Control Channel
    private final MulticastChannel mdbChannel; // Data Backup Channel
    private final MulticastChannel mdrChannel; // Data Restore Channel

    private final ExecutorService threadPoolMC;
    private final ExecutorService threadPoolMDB;
    private final ExecutorService threadPoolMDR;
    private final ScheduledThreadPoolExecutor scheduler;

    public static int MAX_THREADS = 32;
    public static int MAX_THREADS_C = 128;

    public Peer(String[] args) throws IOException {
        this.protocolVersion = args[0];
        this.id = Integer.parseInt(args[1]);
        this.serviceAccessPoint = args[2];

        // Create Peer Internal State
        this.storage = PeerStorage.loadState(this);

        // Create Channels
        this.mcChannel = new MulticastChannel(args[3], Integer.parseInt(args[4]), this);
        this.mdbChannel = new MulticastChannel(args[5], Integer.parseInt(args[6]), this);
        this.mdrChannel = new MulticastChannel(args[7], Integer.parseInt(args[8]), this);

        // Create thread pools
        this.threadPoolMC = Executors.newFixedThreadPool(MAX_THREADS_C);
        this.threadPoolMDB = Executors.newFixedThreadPool(MAX_THREADS);
        this.threadPoolMDR = Executors.newFixedThreadPool(MAX_THREADS);
        this.scheduler = new ScheduledThreadPoolExecutor(MAX_THREADS_C*2);
    }


    public static void main(String[] args) {
        if (args.length != 9) {
            System.out.println("Usage: java Peer <protocolVersion> <peerId> <serviceAccessPoint> <mcAddress> <mcPort> <mdbAddress> <mdbPort> <mdrAddress> <mdrPort>");
            return;
        }

        // Create Peer
        Peer peer;
        try {
            peer = new Peer(args);
        } catch (IOException e) {
            System.err.println("Error creating Peer");
            return;
        }

        // Start RMI
        try {
            PeerInit stub = (PeerInit) UnicastRemoteObject.exportObject(peer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(peer.serviceAccessPoint, stub);
        } catch (RemoteException e) {
            System.err.println("Error starting RMI");
        }

        // Execute Channels
        (new Thread(peer.mcChannel)).start();
        (new Thread(peer.mdbChannel)).start();
        (new Thread(peer.mdrChannel)).start();

        System.out.println(peer);

        // Send WakeyMessage (if enhanced) to alert other peers that this peer is online
        if (peer.isEnhanced()) {
            peer.threadPoolMC.submit(new WakeyWorker(peer));
        }

        // Save peer storage periodically (every minute)
        peer.scheduler.scheduleAtFixedRate(new Thread(peer.storage::saveState), 1, 1, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(peer.storage::saveState));
    }

    public void submitControlThread(Runnable action) {
        this.threadPoolMC.submit(action);
    }

    public Future<Chunk> submitControlThread(Callable<Chunk> action) {
        return this.threadPoolMC.submit(action);
    }

    public void submitBackupThread(Runnable action) {
        this.threadPoolMDB.submit(action);
    }

    public void submitRestoreThread(Runnable action) {
        this.threadPoolMDR.submit(action);
    }

    public void sendControlMessage(Message message) {
        this.mcChannel.sendMessage(message.encode());
    }

    public void sendBackupMessage(Message message) {
        this.mdbChannel.sendMessage(message.encode());
    }

    public void sendRestoreMessage(Message message) {
        this.mdrChannel.sendMessage(message.encode());
    }

    public ScheduledThreadPoolExecutor getScheduler() {
        return this.scheduler;
    }

    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    public int getId() {
        return this.id;
    }

    public PeerStorage getStorage() {
        return this.storage;
    }

    public boolean isEnhanced() {
        return !this.protocolVersion.equals("1.0");
    }

    @Override
    public String toString() {
        return String.format("[PEER] Peer %d with version %s is now running", this.id, this.protocolVersion);
    }

    @Override
    public void backup(String filepath, int replicationDegree) {
        if (this.storage.getStorageFileMap().containsKey(filepath)){
            System.out.println("There is already a backed up version for " +  filepath + ". Delete it before proceeding to a new backup.");
            return;
        }

        try {
            StorageFile storageFile = new StorageFile(filepath, replicationDegree);
            storageFile.backup(this);
            this.storage.getStorageFileMap().put(filepath, storageFile);
        } catch (Exception e) {
            System.err.println("Can't backup file " + filepath);
        }
    }

    @Override
    public void delete(String filepath) {
        StorageFile storageFile = this.storage.getStorageFileMap().get(filepath);
        if (storageFile == null) {
            System.err.println("Can't delete file " + filepath + ", not found");
            return;
        }
        storageFile.delete(this);
        this.storage.getStorageFileMap().remove(filepath);
    }

    @Override
    public void restore(String filepath) throws RemoteException {
        StorageFile storageFile = this.storage.getStorageFileMap().get(filepath);
        if (storageFile == null) {
            System.err.println("Can't restore file " + filepath + ", not found");
            return;
        }

        try {
            storageFile.restore(this);
        } catch (Exception e) {
            System.err.println("Error restoring file " + filepath);
        }
    }

    @Override
    public void reclaim(double maxKBytes) throws RemoteException {
        this.storage.reclaim(this, maxKBytes);
    }

    @Override
    public String state() throws RemoteException {
        return this.storage.toString();
    }
}
