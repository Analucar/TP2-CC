import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import static java.lang.System.currentTimeMillis;

public class HttpGw {

    public static void main(String[] args) throws IOException {

        InetAddress address = InetAddress.getLocalHost();

        Transmitter send = new Transmitter(8888, address);

        send.transmitPackage(args[0]);

        String[] token = args[0].split("/", -1);

        // receber informação

        List<Byte> content = new ArrayList<>();

        Package output = send.receiverPackage();
        /*

        long start = currentTimeMillis();
        long current = currentTimeMillis();
        Package output = null;

        while(current < start + 2000 && output == null){
            current = currentTimeMillis();
            output = send.receiverPackage();
        }

        if(output == null){
            send.transmitPackage(args[0]);
        }

         */

        // offset maior que o comprimento

        int offset = output.getOffset();
        byte[] data = output.getDataBytes();
        boolean fragmentado = output.isFragmentado();

        if (!fragmentado) {

            OutputStream fout = new FileOutputStream("/home/luisa/Desktop/CC/Banana/" + token[token.length - 1]);

            fout.write(data);
            fout.close();
        } else {

            while (fragmentado) {

                for (int i = 0; i < data.length; i++) {
                    content.add(data[i]);
                }

                output = send.receiverPackage();

                offset = output.getOffset();
                data = output.getDataBytes();
                fragmentado = output.isFragmentado();
            }


            Byte[] bytes = content.toArray(new Byte[content.size()]);
            byte[] res = new byte[bytes.length];

            int j = 0;
            // Unboxing Byte values. (Byte[] to byte[])
            for (Byte b : bytes) {
                res[j++] = b.byteValue();
            }

            OutputStream fout = new FileOutputStream("/home/luisa/Desktop/CC/Banana/" + token[token.length - 1]);

            fout.write(res);
            fout.close();

        }

        File file = new File("/home/luisa/Desktop/CC/Banana/" + token[token.length - 1]);
        file.setExecutable(true);

        System.out.println(address);

    }

}
