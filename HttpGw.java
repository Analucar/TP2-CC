import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class HttpGw {

    final static int WORKERS_PER_CONNECTION = 1;

    public static void main(String[] args) throws IOException, InterruptedException {

        Map<Integer, Package[]> pedidos = new HashMap<>();
        Map<Integer, Map<InetAddress, Boolean>> fastFileServers = new HashMap<>();
        //Map<Integer,Boolean> fastFileServers = new HashMap<>();
        InetAddress address = InetAddress.getLocalHost();

        //TODO: REMOVER
        //DatagramSocket conection = new DatagramSocket(1234);
        DatagramSocket connection_2 = new DatagramSocket(1234);
        //DatagramSocket connection = new DatagramSocket(8888,InetAddress.getByName("10.1.1.1"));

        /*
        instlar java
        descomentar esta parte e retirar o address de cima
        compilar javac
        correr o core e correr programa
        java

        InetAddress inetAddress = InetAddress.getLocalHost();

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
            if(!netint.isLoopback() || !netint.isUp()){
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for(InetAddress inetA : Collections.list(inetAddresses)){
                    if(inetA instanceof Inet4Address)
                        inetAddress = inetA;
                }
            }
        }
         */


        System.out.println("HttpGw no endereco: " + address + "\n");

        Thread serverUpdate = new Thread(new ServerUpdater(connection_2, fastFileServers));
        serverUpdate.start();
        // conecção com os clientes com http
        ServerSocket serverSock = new ServerSocket(8080, 0, address);

        //inicia fio de conecção
        Thread cliente = new Thread(new ClienteListener(serverSock, pedidos, fastFileServers));

        cliente.start();
        cliente.join();

    }

    public static class ClienteListener implements Runnable {

        private ServerSocket serverSock;
        private Map<Integer, Package[]> pedidos;
        private Map<Integer, Map<InetAddress, Boolean>> fastFileServers;

        public ClienteListener(ServerSocket serverSock, Map<Integer, Package[]> pedidos, Map<Integer, Map<InetAddress, Boolean>> fastFileServers) {

            this.serverSock = serverSock;
            this.pedidos = pedidos;
            this.fastFileServers = fastFileServers;
        }

        public void run() {

            int count = 0;

            try {
                while (true) {
                    //TaggedConnection tcs = new TaggedConnection(s);
                    Socket sock = serverSock.accept();

                    //System.out.println(send.getInetAddress());
                    GwWorker st = new GwWorker(sock, pedidos, fastFileServers);

                    for (int i = 0; i < WORKERS_PER_CONNECTION; ++i) {
                        new Thread(st).start();
                        System.out.println("Create thread " + count + "\n");
                        count++;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

