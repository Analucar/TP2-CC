import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class Transmitter {

    public static void main(String[] args) throws UnknownHostException, SocketException, IOException  {

        byte[] sendData = new byte[32768];
        byte[] receiveData = new byte[32768];

        InputStream f = new FileInputStream("/home/luisa/Desktop/CC/banana");
        int n = f.read(sendData);
        f.close();

        InetAddress address = InetAddress.getLocalHost();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, 12345);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(packet);

        System.out.println(InetAddress.getLocalHost().getHostAddress());

        datagramSocket.receive(receivePacket);
        String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("RECEIVED: " + sentence);
    }
}