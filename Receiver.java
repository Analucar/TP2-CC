import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;

public class Receiver {

    public static void main(String[] args) {
        int port = 12345;
        new Receiver().run(port);
    }

    public void run(int port) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            byte[] receiveData = new byte[32768];
            String sendString = "confirm";
            byte[] sendData = sendString.getBytes("UTF-8");
            OutputStream fout = new FileOutputStream("/home/luisa/Desktop/CC/Banana/banana");
            int n;

            System.out.printf("Listening on udp:%s:%d%n",
                    InetAddress.getLocalHost().getHostAddress(), port);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                serverSocket.receive(receivePacket);
                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("RECEIVED: " + sentence);

                // now send acknowledgement packet back to sender

                fout.write(receivePacket.getData());
                fout.close();

                File f = new File("/home/luisa/Desktop/CC/Banana/banana");
                f.setExecutable(true);

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        receivePacket.getAddress(), receivePacket.getPort());
                serverSocket.send(sendPacket);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        // should close serverSocket in finally block
    }
}