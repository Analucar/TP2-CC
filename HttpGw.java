import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HttpGw {

    final static int WORKERS_PER_CONNECTION = 1;

    public static void main(String[] args) throws IOException, InterruptedException {

        Map<Integer, Package[]> pedidos = new HashMap<>();
        Map<Integer, Map<InetAddress, Boolean>> fastFileServers = new HashMap<>();
        //Map<Integer,Boolean> fastFileServers = new HashMap<>();

        DatagramSocket conection = new DatagramSocket(1234);

        InetAddress address = InetAddress.getLocalHost();

        System.out.println("HttpGw no endereco: " + address + "\n");

        System.out.println("Procura de servidores");

        conection.setSoTimeout(5000);
        while (true) {
            try {
                byte[] receiveData = new byte[61440];
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

                //conecão broadcast com os servidor
                conection.receive(packet);

                System.out.println(packet.getAddress());

                Package pacote = new Package(packet.getData());

                //porta do servidor
                String msg = pacote.getData();

                Map<InetAddress, Boolean> info = new HashMap<>();
                info.put(packet.getAddress(), true);

                // associa porta do servidor á disponibilidade deste
                fastFileServers.put(Integer.valueOf(msg), info);
                //fastFileServers.put(Integer.valueOf(msg),true);

                System.out.println("Servidor encontrado na porta " + msg);

            } catch (SocketTimeoutException e) {
                break;
            }
        }
        System.out.println("Servidores Encontrados\n");

        // conecção com os clientes com http
        ServerSocket serverSock = new ServerSocket(8080, 0, InetAddress.getByName("127.0.0.1"));

        //inicia fio de conecção
        Thread cliente = new Thread(new ClienteListener(serverSock, address, pedidos, fastFileServers));

        cliente.start();
        cliente.join();

    }

    public static class ClienteListener implements Runnable {

        private ServerSocket serverSock;
        private InetAddress address;
        private Map<Integer, Package[]> pedidos;
        private Map<Integer, Map<InetAddress, Boolean>> fastFileServers;

        public ClienteListener(ServerSocket serverSock, InetAddress address, Map<Integer, Package[]> pedidos, Map<Integer, Map<InetAddress, Boolean>> fastFileServers) {

            this.serverSock = serverSock;
            this.address = address;
            this.pedidos = pedidos;
            this.fastFileServers = fastFileServers;
        }

        public void run() {

            int count = 0;

            try {
                while (true) {
                    //TaggedConnection tcs = new TaggedConnection(s);
                    Socket sock = serverSock.accept();
                    Map<InetAddress, Integer> result = getServerLivre();
                    Transmitter send = null;
                    InetAddress endereco = null;

                    for (Map.Entry<InetAddress, Integer> tuplo : result.entrySet()) {
                        //conecção com os fast file servers
                        send = new Transmitter(tuplo.getValue(), tuplo.getKey());
                        endereco = tuplo.getKey();

                    }

                    send.creatConnection();
                    //System.out.println(send.getInetAddress());
                    ServerWorker st = new ServerWorker(send, endereco, sock, pedidos, fastFileServers);

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

        private Map<InetAddress, Integer> getServerLivre() {

            Map<InetAddress, Integer> res = new HashMap<>();

            for (Map.Entry<Integer, Map<InetAddress, Boolean>> entry : fastFileServers.entrySet()) {

                Map<InetAddress, Boolean> info = entry.getValue();

                for (Map.Entry<InetAddress, Boolean> cont : info.entrySet()) {

                    if (cont.getValue()) {
                        cont.setValue(false);
                        res.put(cont.getKey(), entry.getKey());
                        return res;
                    }
                }
            }

            while (true) {
                for (Map.Entry<Integer, Map<InetAddress, Boolean>> entry : fastFileServers.entrySet()) {

                    Map<InetAddress, Boolean> info = entry.getValue();

                    for (Map.Entry<InetAddress, Boolean> cont : info.entrySet()) {

                        if (cont.getValue()) {
                            cont.setValue(false);
                            res.put(cont.getKey(), entry.getKey());
                            return res;
                        }
                    }
                }
            }

            //return port;
        }
    }

    public static class ServerWorker implements Runnable {

        private Transmitter send;
        private Socket sock;
        private InetAddress address;
        private Map<Integer, Package[]> pedidos;
        private Map<Integer, Map<InetAddress, Boolean>> fastFileServers;

        public ServerWorker(Transmitter send, InetAddress address, Socket sock, Map<Integer, Package[]> pedidos, Map<Integer, Map<InetAddress, Boolean>> fastFileServers) {
            this.send = send;
            this.address = address;
            this.pedidos = pedidos;
            this.sock = sock;
            this.fastFileServers = fastFileServers;
        }

        public void run() {

            InputStream sis = null;
            String request = null;
            BufferedReader br = null;
            try {
                sis = sock.getInputStream();
                br = new BufferedReader(new InputStreamReader(sis));
                request = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {

                try {
                    System.out.println(request + "\n");
                    String[] requestParam = request.split(" ");
                    String path = requestParam[1];
                    String[] realPath = path.split("/");
                    //System.out.println(realPath[1] + "\n");

                    int max = 5000;
                    int min = 1000;
                    int range = max - min + 1;

                    int idPacket = (int) (Math.random() * range) + min;

                    //envio do pedido para os servidores
                    send.transmitPackage(realPath[1], 1, idPacket, 0);

                    int totalLength = 60000;

                    // receber informação
                    Package conteudo = null;
                    int tam, idPedido, falta;
                    tam = 0;
                    idPedido = 0;
                    int flag = 0;
                    int offset = 0;

                    // ao fim do timeout voltamos a pedir o ficheiro...pode-se implementar contador

                    send.timeout(1000);
                    while (true) {
                        try {
                            conteudo = send.receiverPackage();
                            offset = conteudo.getOffset();
                            falta = conteudo.getResto();
                            idPedido = conteudo.getIdPackage();
                            tam = falta + 1 + (offset / totalLength);
                            break;
                        } catch (SocketTimeoutException e) {
                            // resend
                            send.transmitPackage("RENVIO", 1, idPacket, 0);
                            //System.out.println(send.isConnected());
                        } catch (PortUnreachableException s) {
                            System.out.println("PERDA DE CONECÇÃO");
                            throw new Exception();
                        }
                    }

                    Package[] pacotes = new Package[tam];

                    for (int a = 0; a < tam; a++) {
                        pacotes[a] = null;
                    }

                    int indice = offset / totalLength;

                    pacotes[indice] = conteudo;

                    boolean fragmentado = conteudo.isFragmentado();

                    while (fragmentado && ((offset / totalLength) + 1 != tam - 1)) {
                        send.timeout(1000);
                        try {
                            conteudo = send.receiverPackage();
                            offset = conteudo.getOffset();
                            falta = conteudo.getResto();
                            idPedido = conteudo.getIdPackage();
                            pacotes[(offset / totalLength)] = conteudo;
                            fragmentado = conteudo.isFragmentado();
                        } catch (SocketTimeoutException e){
                            fragmentado = conteudo.isFragmentado();
                        }catch (PortUnreachableException s) {
                            System.out.println("PERDA DE CONECÇÃO");
                            throw new Exception();
                        }
                    }

                    int a = 0;
                    int RETRIES = 0;
                    while (a < pacotes.length) {

                        if (pacotes[a] == null) {
                            String res = "RENVIO";
                            send.transmitPackage(res, 1, idPedido, a * totalLength);
                            send.timeout(1000);
                            while (true) {
                                try {
                                    conteudo = send.receiverPackage();
                                    offset = conteudo.getOffset();
                                    falta = conteudo.getResto();
                                    idPedido = conteudo.getIdPackage();
                                    pacotes[(offset / totalLength)] = conteudo;
                                    //a++;
                                    break;
                                } catch (SocketTimeoutException e) {
                                    send.transmitPackage(res, 1, idPedido, a * totalLength);
                                } catch (PortUnreachableException s) {
                                    System.out.println("PERDA DE CONECÇÃO");
                                    throw new Exception();
                                }
                            }

                            RETRIES++;

                        } else {
                            a++;
                        }
                    }

                    List<Byte> content = new ArrayList<>();
                    send.transmitPackage("ACK", 2, idPedido, 0);
                    pedidos.put(idPedido, pacotes);

                    send.timeout(1000);
                    while (true) {
                        try {
                            conteudo = send.receiverPackage();

                            if (!conteudo.getData().equals("LIVRE")) {
                                throw new SocketTimeoutException();
                            } else {
                                int porta = conteudo.getIdPackage();
                                InetAddress add = send.getInetAddress();

                                Map<InetAddress, Boolean> info = new HashMap<>();
                                info.put(add, true);
                                fastFileServers.put(porta, info);

                                send.transmitPackage("ACK LIVRE", 2, 00000, 0);
                                break;
                            }

                        } catch (SocketTimeoutException e) {
                            // resend
                            send.transmitPackage("REENVIO LIVRE", 1, 00000, 0);

                        } catch (PortUnreachableException s) {
                            System.out.println("PERDA DE CONECÇÃO");
                            throw new Exception();
                        }
                    }

                    // envio dos bytes para o cliente http

                    System.out.println("FIM DE TRANSMIÇÃO");

                    for (int b = 0; b < pacotes.length; b++) {

                        byte[] data = pacotes[b].getDataBytes();

                        for (int i = 0; i < data.length; i++) {
                            content.add(data[i]);
                        }
                    }

                    Byte[] bytes = content.toArray(new Byte[content.size()]);
                    byte[] res = new byte[bytes.length];

                    int j = 0;
                    // Unboxing Byte values. (Byte[] to byte[])
                    for (Byte b : bytes) {
                        res[j++] = b;
                    }

                    DataOutputStream out2 = new DataOutputStream(sock.getOutputStream());

                    out2.write(res);
                    out2.flush();

                    br.close();
                    out2.close();

                    System.out.println("FIM TRANSMICAO: " + realPath[1] + "\n");
                    break;


                } catch (Exception e) {
                    //send.disconected();

                    //* desnecessário
                    changeState(send.getPort());

                    Map<InetAddress, Integer> result = getServerLivre();

                    for (Map.Entry<InetAddress, Integer> tuplo : result.entrySet()) {
                        //conecção com os fast file servers
                        send.setTransmission(tuplo.getValue(), tuplo.getKey());
                        address = tuplo.getKey();

                    }
                    send.creatConnection();

                    //send.close();
                    //e.printStackTrace();
                }
            }
        }

        private void changeState(Integer port) {

            Map<InetAddress, Boolean> res = fastFileServers.get(port);

            for (Map.Entry<InetAddress, Boolean> cont : res.entrySet()) {

                cont.setValue(false);
            }

        }

        private Map<InetAddress, Integer> getServerLivre() {

            Map<InetAddress, Integer> res = new HashMap<>();

            for (Map.Entry<Integer, Map<InetAddress, Boolean>> entry : fastFileServers.entrySet()) {

                Map<InetAddress, Boolean> info = entry.getValue();

                for (Map.Entry<InetAddress, Boolean> cont : info.entrySet()) {

                    if (cont.getValue()) {
                        cont.setValue(false);
                        res.put(cont.getKey(), entry.getKey());
                        return res;
                    }
                }
            }

            while (true) {
                for (Map.Entry<Integer, Map<InetAddress, Boolean>> entry : fastFileServers.entrySet()) {

                    Map<InetAddress, Boolean> info = entry.getValue();

                    for (Map.Entry<InetAddress, Boolean> cont : info.entrySet()) {

                        if (cont.getValue()) {
                            cont.setValue(false);
                            res.put(cont.getKey(), entry.getKey());
                            return res;
                        }
                    }
                }
            }
            //return port;
        }
    }
}

