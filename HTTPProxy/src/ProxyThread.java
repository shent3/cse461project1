import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Created by tongshen on 2/12/17.
 */
public class ProxyThread extends Thread {
    private static final String CONNECTION_METHOD = "CONNECT";
    private static final String GET_METHOD = "GET";
    private static final String HOST_TAG = "Host: ";
    private static final String CONNECTION_TAG = "Connection: ";

    private DateTimeFormatter dtf;
    private Socket socket;

    public ProxyThread(Socket socket) {
        this.socket = socket;
        this.dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    }

    @Override
    public void run() {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer();
            String request = reader.readLine();
            if (request == null || request.isEmpty()) {
                return;
            }
            sb.append(request + "\r\n");
            System.out.println(dtf.format(LocalDateTime.now()) + " - >>> " + request);
            String[] requestTokens = request.split(" ");
            String host = null;
            int port = 80;
            for (request = reader.readLine(); request != null && !request.isEmpty(); request = reader.readLine()) {
                if (request.startsWith(CONNECTION_TAG)) {
                    if (!requestTokens[0].equals(CONNECTION_METHOD)) {
                         request = CONNECTION_TAG + "close";
                    }
                } else if (request.startsWith(HOST_TAG)) {
                    String[] hostTokens = request.substring(request.indexOf(" ") + 1).split(":");
                    host = hostTokens[0];
                    if (hostTokens.length == 2) {
                        port = Integer.parseInt(hostTokens[1]);
                    }
                }
                sb.append(request + "\r\n");
            }
            if (requestTokens[0].equalsIgnoreCase(CONNECTION_METHOD)) {
                connect(host, port, sb);
            } else {//if (requestTokens[0].equalsIgnoreCase(GET_METHOD)) {
                nonConnect(host, port, sb);
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(String host, int port, StringBuffer buffer) throws IOException {
        try {
            Socket socket2 = new Socket(host, port);
            OutputStream s2Out = socket2.getOutputStream();
            OutputStream sOut = socket.getOutputStream();
            PrintWriter socketout = new PrintWriter(sOut);
            socketout.write("HTTP/1.0 200 \r\n\r\n");
            socketout.flush();
            new Tunnel(socket.getInputStream(), s2Out).start();
            new Tunnel(socket2.getInputStream(), sOut).start();
        } catch (IOException ioe) {
            System.out.println("UnknownHost: " + ioe.getMessage());
            new OutputStreamWriter(socket.getOutputStream()).write("HTTP/1.0 502 Bad Gateway \r\n\r\n");
        }
    }

    private class Tunnel extends Thread {
        InputStream from;
        OutputStream to;

        Tunnel(InputStream from, OutputStream to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    byte[] buf = new byte[32767];
                    int numOfBytes = from.read(buf);
                    if (numOfBytes == -1) {
                        break;
                    }
                    to.write(buf, 0, numOfBytes);
                    to.flush();
                }
                from.close();
                to.close();
            } catch (IOException ioe) {

            }
        }
    }

    private void nonConnect(String host, int port, StringBuffer buffer) throws IOException {
        try {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            Socket socket2 = new Socket(host, port);
            InputStream serverResponse = socket2.getInputStream();
            PrintWriter clientRequest = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream()));
            clientRequest.print(buffer.toString() + "\r\n");
            clientRequest.flush();
            byte[] buf = new byte[32767];
            int numOfBytes = 0;
            try {
                numOfBytes = serverResponse.read(buf);
            } catch (SocketException se) {}
            while (numOfBytes > 0) {
                try {
                    outputStream.write(buf, 0, numOfBytes);
                    outputStream.flush();
                } catch (SocketException se) {}
                numOfBytes = serverResponse.read(buf);
            }
            clientRequest.close();
            serverResponse.close();
            socket2.close();
        } catch (UnknownHostException ioe) {
            System.out.println("UnknownHost: " + ioe.getMessage());
        }
    }
}
