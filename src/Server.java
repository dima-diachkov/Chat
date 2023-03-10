import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(7778 );
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            try {
                shutdown();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public void shutdown() throws IOException {
        done = true;
        pool.shutdown();
        if (!server.isClosed()) {
            server.close();
        }
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " connected!");
                sendToAll(nickname + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/quit")) {
                        sendToAll(nickname + " left the chat!");
                        shutdown();
                    } else {
                        sendToAll(nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                try {
                    shutdown();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        public void sendToAll(String message) {
            for (ConnectionHandler ch : connections) {
                if (ch != null) {
                    ch.sendMassage(message);
                }
            }
        }

        public void sendMassage(String message) {
            out.println(message);
        }

        public void shutdown() throws IOException {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
