package com.scasp.network;

import com.scasp.app.SampleData;
import com.scasp.concurrent.RequestProcessor;
import com.scasp.service.AcademicService;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Multi-client TCP gateway. Each accepted connection becomes a task on the Stage 2 {@link RequestProcessor}
 * (bounded, back-pressured); when the processor is full the client immediately gets {@code ERR 503 BUSY}.
 * Each connection may send several commands until QUIT, disconnect or the idle timeout.
 */
public class CampusTcpServer implements AutoCloseable {
    private final ServerSocket server;
    private final RequestProcessor processor;
    private final CampusProtocol protocol;
    private final int idleTimeoutMillis;
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private volatile boolean running = true;
    private volatile Consumer<String> trace = s -> {};

    /** Original constructor: sample data, 4 workers, 5 s idle timeout. */
    public CampusTcpServer(int port) throws IOException {
        this(SampleData.load(new AcademicService()), port, new RequestProcessor(16, 4), 5000);
    }
    public CampusTcpServer(AcademicService service, int port, RequestProcessor processor, int idleTimeoutMillis) throws IOException {
        this.server = new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
        this.processor = processor;
        this.protocol = new CampusProtocol(service);
        this.idleTimeoutMillis = idleTimeoutMillis;
    }
    /** Binds to all interfaces (for lab demos across two computers). */
    public static CampusTcpServer onAllInterfaces(AcademicService service, int port, RequestProcessor processor, int idleTimeoutMillis) throws IOException {
        return new CampusTcpServer(service, processor, idleTimeoutMillis, new ServerSocket(port));
    }
    private CampusTcpServer(AcademicService service, RequestProcessor processor, int idle, ServerSocket ss) {
        this.server = ss; this.processor = processor; this.protocol = new CampusProtocol(service); this.idleTimeoutMillis = idle;
    }

    public int getPort() { return server.getLocalPort(); }
    public void setTrace(Consumer<String> trace) { this.trace = trace; }

    /** Blocking accept loop (runs until close()). */
    public void start() throws IOException {
        while (running) {
            Socket client;
            try { client = server.accept(); }
            catch (SocketException e) { if (!running) return; throw e; }
            clients.add(client);
            try {
                processor.submit(() -> { handle(client); return null; }, 200, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                reject(client);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); reject(client); return;
            }
        }
    }
    /** Starts the accept loop on a background thread and returns immediately. */
    public Thread startInBackground() {
        Thread t = Thread.ofPlatform().name("scasp-tcp-accept").daemon(true).start(() -> {
            try { start(); } catch (IOException e) { if (running) e.printStackTrace(); }
        });
        return t;
    }

    private void reject(Socket client) {
        try (client; Writer w = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)) {
            w.write(CampusProtocol.err(503, "BUSY", "Server busy, try again later") + "\n"); w.flush();
        } catch (IOException ignored) { } finally { clients.remove(client); }
    }

    private void handle(Socket socket) {
        String peer = socket.getRemoteSocketAddress().toString();
        try (socket;
             InputStream in = new BufferedInputStream(socket.getInputStream());
             Writer out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(idleTimeoutMillis);
            while (running) {
                String line;
                try {
                    line = readLine(in);
                } catch (LineTooLongException e) {
                    send(out, peer, CampusProtocol.err(413, "LINE_TOO_LONG", "Max " + CampusProtocol.MAX_LINE + " bytes"));
                    return;
                } catch (SocketTimeoutException e) {
                    send(out, peer, CampusProtocol.err(408, "IDLE_TIMEOUT", "Closing idle connection"));
                    return;
                }
                if (line == null) { trace.accept(peer + " disconnected"); return; }
                trace.accept(peer + " C: " + line);
                String response = protocol.handle(line);
                send(out, peer, response);
                if (response.equals("OK BYE")) return;
            }
        } catch (IOException e) {
            trace.accept(peer + " I/O error: " + e.getMessage());
        } finally {
            clients.remove(socket);
        }
    }
    private void send(Writer out, String peer, String response) throws IOException {
        out.write(response); out.write('\n'); out.flush();
        trace.accept(peer + " S: " + response);
    }

    static final class LineTooLongException extends IOException { LineTooLongException() { super("line too long"); } }

    /** Reads one LF-terminated UTF-8 line with a hard length limit; returns null on clean EOF. */
    static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (buf.size() >= CampusProtocol.MAX_LINE) throw new LineTooLongException();
            buf.write(b);
        }
        if (b == -1 && buf.size() == 0) return null;
        String s = buf.toString(StandardCharsets.UTF_8);
        return s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
    }

    public int connectedClients() { return clients.size(); }

    /** Orderly shutdown: stop accepting, end client sessions, then shut the processor down. */
    @Override public void close() throws IOException {
        running = false;
        server.close();
        for (Socket s : clients) { try { s.shutdownInput(); } catch (IOException ignored) { } }
        try { processor.shutdown(2, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        for (Socket s : clients) { try { s.close(); } catch (IOException ignored) { } }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5050;
        AcademicService service = SampleData.load(new AcademicService());
        try (CampusTcpServer s = onAllInterfaces(service, port, new RequestProcessor(16, 4), 30000)) {
            s.setTrace(System.out::println);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { s.close(); } catch (IOException ignored) { } }));
            System.out.println("SCASP TCP gateway on port " + s.getPort() + " (commands: " + CampusProtocol.COMMANDS + ")");
            s.start();
        }
    }
}
