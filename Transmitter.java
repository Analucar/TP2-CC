import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class Transmitter {

    int port;
    InetAddress address;
    DatagramSocket datagramSocket;

    public Transmitter(int port, InetAddress address) throws SocketException {
        this.port = port;
        this.address = address;
        this.datagramSocket = new DatagramSocket();
    }

    /*Recebe uma requesta (para já apenas o nome do ficheiro) e comunica-a ao reciever*/
    public void transmitPackage(String request) throws IOException  {

        byte[] sendRequest = request.getBytes();

        Package pacote = new Package(false,123,0,sendRequest);
        byte[] sendData = pacote.serializePackage();

        DatagramPacket sendpacket = new DatagramPacket(sendData, sendData.length, this.address, this.port);
        this.datagramSocket.send(sendpacket);

    }

    /*Recebe o pedido do utilizador, gerado pela função transmitPackage*/
    public Package receiverPackage() throws IOException {

        byte[] receiveData = new byte[64000];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        datagramSocket.receive(receivePacket);

        Package pacote = new Package(receivePacket.getData());

        return pacote;
    }
}