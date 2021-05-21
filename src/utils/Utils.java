package utils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Utils {
    public static final int CHORD_M = 8;
    public static final int CHORD_MAX_PEERS = (int) Math.pow(2, Utils.CHORD_M);

    public static int generateId(InetSocketAddress socketAddress){
        String toHash = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch(NoSuchAlgorithmException e) {
            return -1;
        }

        byte[] hashed = messageDigest.digest(toHash.getBytes(StandardCharsets.UTF_8));
        return new String(hashed).hashCode() & 0x7fffffff % CHORD_MAX_PEERS;
    }
}
