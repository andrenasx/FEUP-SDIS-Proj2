package utils;

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

    public static String generateHashForFile(String filename, BasicFileAttributes attributes) {
        String creationTime = String.valueOf(attributes.creationTime().toMillis());
        String modificationTime = String.valueOf(attributes.lastModifiedTime().toMillis());
        return hashToASCII(filename + creationTime + modificationTime);
    }

    /**
     * Method to hash a string and convert it to ASCII encoding
     *
     * @param string String to be hashed and converted
     * @return The Hash on ASCII encoding
     */
    public static String hashToASCII(String string) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method to convert a byte array to the Hexadecimal representation
     *
     * @param bytes Byte array to be converted to String on a Hexadecimal Representation
     * @return The byte array converted to a Hexadecimal String
     */
    private static String bytesToHex(byte[] bytes) {
        char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String prettySize(double bytes) {
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
