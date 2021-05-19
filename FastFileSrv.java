import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class FastFileSrv {


    public static void main(String[] args) throws SocketException {

        Receiver receive = new Receiver(8888);

        receive.run();

    }



}
