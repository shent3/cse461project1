import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by tongshen on 2/12/17.
 */
public class ClientThread extends Thread {
    private Socket socket;
    private String host;
    private int port;
    private static final String CONNECTION_TAG = "CONNECTION:";
    private static final String HOST_TAG = "host:";
    private static final String CONNECTION_CLOSE = "\r\n";

    public ClientThread(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        port = 80;

        try {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(socket.getInputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String request = reader.readLine();

            while (request != null && request.length() > 0) {
                System.out.println("HTTP request: " + request);
                request = reader.readLine();

                String[] requestTokens = request.split(" ");
                String requestHeader = requestTokens[0];
                switch (requestHeader.toUpperCase()) {
                    case CONNECTION_CLOSE:
                        request = CONNECTION_CLOSE;
                        break;
                    case HOST_TAG:
                        host = "";
                        for (int i = 1; i < requestTokens.length; i++)
                            host += requestTokens[i];

                        // retrieve the port number from the HTTP, if specified
                        String[] hostParts = host.split(":");
                        if (hostParts.length == 2)
                            port = Integer.valueOf(hostParts[1]).intValue();

                        request = "HOST" + host;
                }
//                if (requestHeader.equalsIgnoreCase(CONNECTION_TAG)) {
//                    request = CONNECTION_CLOSE;
//                } else if ((requestTokens[0].toLowerCase()).equals(HOST_TAG)) {
//                    host = "";
//
//                    // retrieve the host from the HTTP
//                    for (int i = 1; i < requestTokens.length; i++)
//                        host += requestTokens[i];
//
//                    // retrieve the port number from the HTTP, if specified
//                    String[] hostParts = host.split(":");
//                    if (hostParts.length == 2)
//                        port = Integer.valueOf(hostParts[1]).intValue();
//
//                    request = "HOST" + host;
//                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
