import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by tongshen on 2/12/17.
 */

public class HTTPProxy {
    public static int portNumber;

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        runProxy(scan);
    }

    public static void runProxy(Scanner scan) {
        while (true) {
            if (scan.hasNextInt()) {
                portNumber = scan.nextInt();
            } else {
                System.out.println("Invalid port number.");
            }

            if (portNumber < 0 || portNumber > 65536) {
                System.out.println("Invalid port number.");
            } else {
                break;
            }
        }

        ServerSocket socket = null;
        Thread thread = null;
        try {
            socket = new ServerSocket(portNumber);
            thread = new ClientThread(socket.accept());
        } catch (Exception e) {
            e.printStackTrace();
        }

        thread.run();

    }
}
