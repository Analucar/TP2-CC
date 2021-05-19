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
            byte[] receiveData = new byte[61440];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            int offset = 0;

            while (true) {

                serverSocket.receive(receivePacket);
                Package pacote = new Package(receivePacket.getData());

                InputStream fout = new FileInputStream(pacote.getData());
                File f = new File(pacote.getData());

                long size = f.length();
                byte[] file = new byte[(int) size];
                // ir enviando pacotes de no maximo 61440 até atingir o fim

                int length = fout.read(file);
                fout.close();

                int totalLength = 60000;

                Package pacoteSend = null;

                if(size < totalLength){

                    byte[] copyFile = new byte[(int) size];

                    for (int i = 0; i < size; i++) {
                        copyFile[i] = file[i];
                    }

                    pacoteSend = new Package(false, pacote.idPackage, 0, copyFile); // 2 - nao fragmentado

                    byte[] sendData = pacoteSend.serializePackage();

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                    serverSocket.send(sendPacket);
                }
                else {

                    int fragmentos = (int) size/totalLength + 1;
                    System.out.println("fragemntos: " + fragmentos);

                    for (int indice = 0; indice < fragmentos; indice++) {

                        if(indice == fragmentos-1) {

                            byte[] copyFile = new byte[(int) size%totalLength];

                            System.out.println(size%totalLength);

                            int j = 0;
                            for (int i = offset; j < size%totalLength; i++, j++) {
                                copyFile[j] = file[i];
                            }
                            System.out.println("último fragmento");
                            pacoteSend = new Package(false, pacote.idPackage, offset, copyFile);
                            System.out.println("Data size: " + copyFile.length);
                        }
                        else{
                            offset = totalLength*indice;
                            byte[] copyFile = new byte[totalLength];
                            int j = 0;

                            for (int i = offset; j < totalLength; i++, j++) {
                                copyFile[j] = file[i];
                            }

                            System.out.println("Fragmento");

                            pacoteSend = new Package(true, pacote.idPackage, offset, copyFile);
                            System.out.println("send data: " +copyFile.length);
                        }

                        byte[] sendData = pacoteSend.serializePackage();

                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                        System.out.println("send packet: " +sendPacket.getData().length);

                        serverSocket.send(sendPacket);
                    }

                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        // should close serverSocket in finally block
    }
}