import java.io.IOException;
import java.net.InetAddress;

public class FastFileSrv {


    public static void main(String[] args) throws IOException {

        Receiver receive = new Receiver(12345);

        receive.run();

    }

}
