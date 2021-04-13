import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class HttpGw {

    public static void main(String[] args) throws IOException {

        InetAddress address = InetAddress.getLocalHost();

        Transmitter send = new Transmitter(12345,address);

        send.transmitPackage(args[0]);

        byte[] file = send.receiverPackage();

        OutputStream fout = new FileOutputStream("/home/luisa/Desktop/CC/Banana/out.txt");

        fout.write(file);
        fout.close();

        // escrever o conteudo do file num ficheiro.
        //String output = new String(file);

        //System.out.println(output);

    }

}
