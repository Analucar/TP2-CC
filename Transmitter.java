import java.io.IOException;
import java.net.*;

public class Transmitter {

    public static void main(String[] args) throws UnknownHostException, SocketException, IOException  {

        String teste = args[0];
        byte[] receiveData = new byte[50];

        byte[] buffer = teste.getBytes();
        InetAddress address = InetAddress.getLocalHost();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 12345);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(packet);

        System.out.println(InetAddress.getLocalHost().getHostAddress());

        datagramSocket.receive(receivePacket);
        String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("RECEIVED: " + sentence);
    }
}