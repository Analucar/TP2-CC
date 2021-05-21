import java.io.*;
import java.net.*;

import static java.lang.Thread.sleep;


public class Receiver {

    private int totalLength = 60000;
    private int port;
    private DatagramSocket serverSocket;

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
                File f = new File(pacote.getData()); // **ATENÇÃO** fazer uma pesquisa por onde esta o ficheiro??

                long size = f.length();
                byte[] file = new byte[(int) size];
                // ir enviando pacotes de no maximo 61440 até atingir o fim

                int length = fout.read(file);
                fout.close();

                Package pacoteSend = null;

                if (size < totalLength) {

                    byte[] copyFile = new byte[(int) size];

                    for (int i = 0; i < size; i++) {
                        copyFile[i] = file[i];
                    }

                    pacoteSend = new Package(false, false, 0, pacote.getIdPackage(), 0, copyFile); // 2 - nao fragmentado

                    byte[] sendData = pacoteSend.serializePackage();

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                    System.out.println("Data size: " + copyFile.length);
                    serverSocket.send(sendPacket);
                } else {

                    int fragmentos = (int) size / totalLength + 1;
                    System.out.println("fragemntos: " + fragmentos);

                    for (int indice = 0; indice < fragmentos; indice++) {

                        if (indice == fragmentos - 1) {

                            byte[] copyFile = new byte[(int) size % totalLength];

                            System.out.println(size % totalLength);

                            int j = 0;
                            for (int i = offset; j < size % totalLength; i++, j++) {
                                copyFile[j] = file[i];
                            }

                            offset = totalLength * indice;
                            System.out.println("último fragmento");
                            pacoteSend = new Package(false, false, fragmentos - 1 - indice, pacote.getIdPackage(), offset, copyFile);
                            System.out.println("Data size: " + copyFile.length);
                        } else {
                            offset = totalLength * indice;
                            byte[] copyFile = new byte[totalLength];
                            int j = 0;

                            for (int i = offset; j < totalLength; i++, j++) {
                                copyFile[j] = file[i];
                            }

                            System.out.println("Fragmento");


                            pacoteSend = new Package(true, false, fragmentos - 1 - indice, pacote.getIdPackage(), offset, copyFile);
                            System.out.println("send data: " + copyFile.length);
                        }

                        byte[] sendData = pacoteSend.serializePackage();

                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                        System.out.println("send packet: " + sendPacket.getData().length);
                        if (indice%2==0) Thread.sleep(4000);
                        serverSocket.send(sendPacket);
                    }
                }

                boolean ack = false;

                while (!ack) {
                    Package ackPacote = receive();
                    ack = ackPacote.isAck();

                    if (!ack) {
                        int id = ackPacote.getIdPackage();
                        int off = ackPacote.getOffset();
                        enviarPacoteOffset(id, off, file, receivePacket.getAddress(), receivePacket.getPort());
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
        // should close serverSocket in finally block
    }

    private void enviarPacoteOffset(int id, int offset, byte[] f, InetAddress address, int port) throws IOException {

        int tam = 0;
        int size = f.length;

        if (size - offset >= totalLength) {
            tam = totalLength;
        } else tam = size % totalLength;

        byte[] copyFile = new byte[tam];

        int j = 0;
        for (int i = offset; j < tam; i++, j++) {
            copyFile[j] = f[i]; // problema com cast -> offset dá um valor acima dos int o q dá erro na onversão.
        }

        int falta = 0;
        long x = offset;
        while (x <= size) {
            x += totalLength;
            falta++;
        }

        System.out.println("Reenvio fragmento");
        Package pacoteSend = new Package(false, false, falta, id, offset, copyFile);

        System.out.println("Data size: " + copyFile.length);
        System.out.println("falta: " + falta);
        System.out.println("offset: " + offset);

        byte[] sendData = pacoteSend.serializePackage();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        System.out.println("send packet: " + sendPacket.getData().length);
        serverSocket.send(sendPacket);
    }

    public Package receive() throws IOException {

        byte[] receiveData = new byte[64000];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        serverSocket.receive(receivePacket);

        Package pacote = new Package(receivePacket.getData());

        return pacote;
    }
}