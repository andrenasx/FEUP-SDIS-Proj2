package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInit extends Remote {
    void backup(String filepath, int replicationDegree) throws RemoteException;

    void restore(String filepath) throws RemoteException;

    void delete(String filepath) throws RemoteException;

    void reclaim(double diskspace) throws RemoteException;

    String state() throws RemoteException;
}
