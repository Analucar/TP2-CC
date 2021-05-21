import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpGw {

    public static void main(String[] args) throws IOException {

        Map<Integer, Package[]> pedidos = new HashMap<>();
        Map<String,Integer> fastFileServers = new HashMap<>();

        ServerSocket socket = new ServerSocket(12345);
        Socket s = socket.accept();

        DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

        int size = in.readInt();
        System.out.println(size);

        byte[] ping = new byte[size];

        in.readFully(ping);
        String envio = new String(ping);
        String[] info = envio.split(",");
        fastFileServers.put(info[0], Integer.valueOf(info[1]));

        String resposta = "ACK";

        out.writeInt(resposta.length());
        out.write(resposta.getBytes());
        out.flush();

        s.close();

        InetAddress address = InetAddress.getLocalHost();
        Transmitter send = new Transmitter(fastFileServers.get("Server 1"), address);
        ServerSocket serverSock = new ServerSocket(8080, 0, InetAddress.getByName("127.0.0.1"));

        while (true) {

            Socket sock = serverSock.accept();
            InputStream sis = sock.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(sis));
            String request = br.readLine();

            System.out.println(request + "\n");
            String[] requestParam = request.split(" ");
            String path = requestParam[1];
            String[] realPath = path.split("/");
            System.out.println(realPath[1] + "\n");

            send.transmitPackage(realPath[1], 1, 123, 0);

            int totalLength = 60000;

            // receber informação
            Package conteudo = null;
            int tam, idPedido, falta;
            tam = 0;
            idPedido = 0;
            int flag = 0;
            int offset = 0;

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
                    send.transmitPackage(realPath[1], 1, 123, 0);
                }
            }

            Package[] pacotes = new Package[tam];

            for (int a = 0; a < tam; a++) {
                pacotes[a] = null;
            }

            int indice = offset / totalLength;

            pacotes[indice] = conteudo;

            boolean fragmentado = conteudo.isFragmentado();

            while (fragmentado) {
                send.timeout(1000);
                while (true) {
                    try {
                        conteudo = send.receiverPackage();
                        offset = conteudo.getOffset();
                        falta = conteudo.getResto();
                        idPedido = conteudo.getIdPackage();
                        pacotes[(offset / totalLength)] = conteudo;
                        break;
                    } catch (SocketTimeoutException e) {
                        String res = "RENVIO";
                        int num = offset + totalLength;
                        send.transmitPackage(res, 1, idPedido, num);
                    }
                }
                fragmentado = conteudo.isFragmentado();
            }

            for (int a = 0; a < pacotes.length; a++) {

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
                            break;
                        } catch (SocketTimeoutException e) {
                            send.transmitPackage(res, 1, idPedido, a * totalLength);
                        }
                    }
                }
            }

            List<Byte> content = new ArrayList<>();
            send.transmitPackage("ACK", 2, idPedido, 0);
            pedidos.put(idPedido, pacotes);

            for (int a = 0; a < pacotes.length; a++) {

                byte[] data = pacotes[a].getDataBytes();

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

            System.out.println(address);
        }
    }
}

