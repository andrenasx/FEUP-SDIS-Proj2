package sslengine;

import javax.net.ssl.SSLContext;

public class ServerRunnable implements Runnable {
    SSLContext context;
    String host;
    int port;
    SSLEngineServer server;

    public ServerRunnable(SSLContext context, String host, int port) {
        this.context = context;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            server = new SSLEngineServer(context, host, port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Should be called in order to gracefully stop the server.
     */
    public void stop() {
        server.stop();
    }

}