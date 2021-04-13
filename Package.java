import java.io.*;

public class Package {

    int flag;
    int idPackage;
    int offset;
    byte[] data;

    public Package(int flag, int idPackage, int offset, byte[] data) {
        this.flag = flag;
        this.idPackage = idPackage;
        this.offset = offset;
        this.data = data;
    }

    public Package(byte[] data) throws IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);

        this.flag = is.readInt();
        this.idPackage = is.readInt();
        this.offset = is.readInt();

        byte[] dataPacket = new byte[is.readInt()];
        is.readFully(dataPacket);
        this.data = dataPacket;

    }

    public byte[] serializePackage(){

        byte[] serialize = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeInt(flag);
            out.writeInt(idPackage);
            out.writeInt(offset);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            serialize = bos.toByteArray();
            bos.close();
        }catch (Exception e){e.printStackTrace();}

        return serialize;

    }

    public String getData(){

        String result = new String(data,0,data.length);
        return result;

    }



}
