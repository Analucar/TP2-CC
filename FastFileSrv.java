import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class FastFileSrv {


    public static void main(String[] args) throws IOException {

        Socket s = new Socket("localhost", 12345);

        DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

        int port = 8888;

        String ping = "Server 1,"+ port;

        out.writeInt(ping.length());
        out.write(ping.getBytes());
        out.flush();

        int tam = in.readInt();

        byte[] resposta = new byte[tam];

        in.readFully(resposta);

        String info = new String(resposta);

        if(info.contains("ACK")){
            s.close();
            Receiver receive = new Receiver(port);
            receive.run();
        }

    }

}
