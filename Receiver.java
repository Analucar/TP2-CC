import java.io.*;
import java.net.*;

public class Receiver {

    int port;
    DatagramSocket serverSocket;

    public Receiver(int port) throws SocketException {
        this.port = port;
        this.serverSocket = new DatagramSocket(port);
    }

    public void run() {
        try {
            byte[] receiveData = new byte[32768];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                byte[] sendData = new byte[32768];

                serverSocket.receive(receivePacket);
                Package pacote = new Package(receivePacket.getData());

                InputStream fout = new FileInputStream(pacote.getData());

                fout.read(sendData);
                fout.close();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                serverSocket.send(sendPacket);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        // should close serverSocket in finally block
    }
}