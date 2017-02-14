import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Created by tongshen on 2/12/17.
 */
public class ProxyThread extends Thread {
    private Socket socket;
    private static final String CONNECTION_TAG = "CONNECTION:";
    private static final String GET_METHOD = "GET";
    private static final String HOST_TAG = "host:";
    private static final String CONNECTION_CLOSE = "CONNECTION: close";

    public ProxyThread(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        int port = 80;

        try {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(socket.getInputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            String request = reader.readLine();
            if (request != null) {
                LocalDateTime now = LocalDateTime.now();
                System.out.println(dtf.format(now) + " - Proxy listening on " + socket.getLocalAddress() + ": " + socket.getLocalPort());
            }
            String host = null;
            StringBuffer outputBuffer = new StringBuffer();
            while (request != null && request.length() > 0) {
                LocalDateTime now = LocalDateTime.now();
                request.trim();
                String[] requestTokens = request.split(" ");
                String requestHeader = requestTokens[0];
                if (requestHeader.equalsIgnoreCase(GET_METHOD)) {
                    System.out.println(dtf.format(now) + " - >>> " + request);
                } else if (requestHeader.equalsIgnoreCase(CONNECTION_TAG)) {
                    request = CONNECTION_CLOSE;
                } else if (requestHeader.equalsIgnoreCase(HOST_TAG)) {
                    String[] hostTokens = requestTokens[1].split(":");
                    if (hostTokens.length == 2) {
                        port = Integer.parseInt(hostTokens[1]);
                    }
                    host = hostTokens[0];
                    System.out.println(dtf.format(now) + " - >>> CONNECT " + host + ":" + port);
                    request = "HOST: " + host + ":" + port;
                }
                if (outputBuffer != null && request != null) {
                    outputBuffer.append(request + "\r\n");
                }
                request = reader.readLine();
            }
            if (outputBuffer != null) {
                outputBuffer.append("\r\n");
            }
            if (host != null) {
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                System.out.println(host + port);
                Socket socket2 = new Socket(host, port);
                InputStream serverResponse = socket2.getInputStream();
                PrintWriter clientRequest = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream()));
                clientRequest.print(outputBuffer.toString());
                clientRequest.flush();
                byte[] buf = new byte[32767];
                int numOfBytes = serverResponse.read(buf);
                while (numOfBytes != -1) {
                    outputStream.write(buf, 0, numOfBytes);
                    outputStream.flush();
                    numOfBytes = serverResponse.read(buf);
                }
                clientRequest.close();
                serverResponse.close();
                socket2.close();
                outputStream.close();
            }
            reader.close();
        } catch (UnknownHostException e) {
            System.out.println("UnknownHost: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
