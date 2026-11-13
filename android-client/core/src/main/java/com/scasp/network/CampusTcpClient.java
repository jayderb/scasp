package com.scasp.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** TCP client for the SCASP gateway. Supports one-shot commands and multi-command sessions. */
public class CampusTcpClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader in;
    private final Writer out;

    public CampusTcpClient(String host, int port, int timeoutMillis) throws IOException {
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) { socket.close(); throw e; }
    }
    /** Sends one framed command and waits for exactly one response line (null if the server closed). */
    public String send(String command) throws IOException {
        out.write(command); out.write('\n'); out.flush();
        return in.readLine();
    }
    public String readLine() throws IOException { return in.readLine(); }
    @Override public void close() throws IOException { socket.close(); }

    /** Original one-shot API. */
    public static String send(String host, int port, String command) throws IOException {
        try (CampusTcpClient c = new CampusTcpClient(host, port, 3000)) { return c.send(command); }
    }

    public static void main(String[] a) throws Exception {
        String host = System.getProperty("scasp.host", "localhost");
        int port = Integer.getInteger("scasp.port", 5050);
        try (CampusTcpClient c = new CampusTcpClient(host, port, 5000)) {
            for (String cmd : a.length == 0 ? new String[]{"HELP"} : a) {
                System.out.println("C: " + cmd);
                System.out.println("S: " + c.send(cmd));
            }
            c.send("QUIT");
        } catch (ConnectException e) {
            System.err.println("SCASP gateway not reachable at " + host + ":" + port + " (" + e.getMessage() + ")");
        }
    }
}
