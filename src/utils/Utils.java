package utils;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Utils {
    public static final int CHORD_M = 8;
    public static final int CHORD_R = 3;
    public static final int CHORD_MAX_PEERS = (int) Math.pow(2, Utils.CHORD_M);

    public static int generateId(InetSocketAddress socketAddress) {
        String toHash = socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return -1;
        }

        byte[] hashed = messageDigest.digest(toHash.getBytes(StandardCharsets.UTF_8));
        return new String(hashed).hashCode() & 0x7fffffff % CHORD_MAX_PEERS;
    }

    public static String generateHashForFile(String filepath, BasicFileAttributes attributes) {
        // Create string from file metadata
        String bitstring = filepath + attributes.creationTime() + attributes.lastModifiedTime() + attributes.size();

        // Apply SHA256 to the string
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bitstring.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hash));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String convertSize(double bytes) {
        String type = "B";
        if (bytes / 1024 > 1) {
            bytes /= 1024.0;
            type = "KB";
        }
        if (bytes / 1024 > 1) {
            bytes /= 1024.0;
            type = "MB";
        }
        if (bytes / 1024 > 1) {
            bytes /= 1024.0;
            type = "GB";
        }

        return String.format("%.2f %s", bytes, type);
    }
}
