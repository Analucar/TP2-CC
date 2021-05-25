import java.io.*;
import java.net.*;

public class FastFileSrv {

    final static int WORKERS_PER_SERVER = 1;


    public static void main(String[] args) throws InterruptedException, SocketException, UnknownHostException {

        //conecão broadcast para envio da porta especifica

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        DatagramSocket data = new DatagramSocket(port);
        //data.setBroadcast(true);
        data.connect(address, port);



        /*

        int max = 9000;
        int min = 8800;
        int range = max - min + 1;

        int port = (int) (Math.random() * range) + min;

         */

        String msg = String.valueOf("ACK SERVER");

        Package p = new Package(false, true, 0, 00000, 0, msg.getBytes());

        byte[] receiveData = p.serializePackage();
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

        try {
            data.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Recebendo Pedidos na porta: " + port + "\n");

        Receiver receive = new Receiver(port);

        receive.run();


        Thread servidor = new Thread(new ServerListener(receive));

        servidor.start();
        servidor.join();

    }

    public static class ServerListener implements Runnable {

        private Receiver receive;

        public ServerListener(Receiver receive) {
            this.receive = receive;
        }

        public void run() {

            int count = 0;

            FileServerWorker fsw = new FileServerWorker(receive);

            for (int i = 0; i < WORKERS_PER_SERVER; ++i) {
                new Thread(fsw).start();
                System.out.println("\n Thread SERVER " + count + "\n");
            }

        }
    }

    public static class FileServerWorker implements Runnable {

        private Receiver receive;

        public FileServerWorker(Receiver receive) {
            this.receive = receive;
        }

        public void run() {

            receive.run();
        }
    }
}


