import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

public class Transmitter {

    public static void main(String[] args) throws UnknownHostException, SocketException, IOException  {
        // TODO code application logic here
        String teste = "comunicar com o reciever";
        byte[] buffer = teste.getBytes();
        InetAddress address = InetAddress.getLocalHost();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 12345);
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(packet);
        System.out.println(InetAddress.getLocalHost().getHostAddress());
    }
}