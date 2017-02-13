import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by tongshen on 2/12/17.
 */
public class ClientThread extends Thread {
    private Socket socket;
    private static final String CONNECTION_TAG = "CONNECTION:";
    private static final String GET_METHOD = "GET";
    private static final String HOST_TAG = "host:";
    private static final String CONNECTION_CLOSE = "CONNECTION: close";

    public ClientThread(Socket socket) {
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
                System.out.println(dtf.format(now) + ": Proxy listening on " + socket.getLocalAddress() + ": " + socket.getLocalPort());
            }
            StringBuffer outputBuffer = new StringBuffer();
            String host = null;
            while (request != null && request.length() > 0) {
                LocalDateTime now = LocalDateTime.now();

                String[] requestTokens = request.split(" ");
                String requestHeader = requestTokens[0];
                System.out.println(dtf.format(now) + ": " + request);

                if (requestHeader.equalsIgnoreCase(GET_METHOD)) {
//                    System.out.println(dtf.format(now) + ": " + request);
                } else if (requestHeader.equalsIgnoreCase(CONNECTION_TAG)) {
//                    System.out.println(dtf.format(now) + ": " + request);
                } else if (requestHeader.equalsIgnoreCase(HOST_TAG)) {
                    StringBuilder sb = new StringBuilder("");
                    for (int i = 1; i < requestTokens.length; i++)
                        sb.append(requestTokens[i]);

                    String[] hostTokens = sb.toString().split(":");
                    if (hostTokens.length == 2) {
                        port = Integer.valueOf(hostTokens[1]);
                    }
                    host = sb.toString();
                    request = "HOST" + host;
                }


                // append end line tag
                if (outputBuffer != null && request != null) {
                    outputBuffer.append(request + "\r\n");
                }

                if (host != null) {
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    Socket socket2 = new Socket(host, port);
                    InputStream serverResponse = socket2.getInputStream();
                    PrintWriter clientRequest =
                            new PrintWriter(new OutputStreamWriter(socket2.getOutputStream()));

                    // print out the request
                    clientRequest.print(outputBuffer.toString());

                    // flush the writer
                    clientRequest.flush();

                    // a buffer to hold the response from server
                    // and send to the client
                    byte[] buf = new byte[32767];
                    int numOfBytes = serverResponse.read(buf);

                    // keep reading from the buffer until there's nothing to be read
                    while (numOfBytes != -1) {
                        // write the response to client
                        // and flush the writer
                        outputStream.write(buf, 0, numOfBytes);
                        outputStream.flush();

                        // read the next line of the response
                        numOfBytes = serverResponse.read(buf);

                        // cSocket.shutdownOutput(); // for experiment
                        // cSocket.shutdownInput(); // for experiment
                    }

                    // done with sending request and getting response,
                    // so close all the things we created for the connection
                    clientRequest.close();
                    serverResponse.close();
                    socket2.close();
                    outputStream.close();
                }

                request = reader.readLine();
            }
            reader.close();

        } catch (UnknownHostException e) {
            System.out.println("UnknownHost: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
