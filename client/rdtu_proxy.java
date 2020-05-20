import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class rdtu_proxy implements Runnable {
    byte[] data;
    int timeout;
    DatagramSocket clientSocket;
    InetAddress ip_addr;
    int rport;
    int seq_no;
    Thread t;

    rdtu_proxy(byte[] data, int timeout, DatagramSocket clientSocket, InetAddress ip_addr, int rport, int seq_no) {
        this.data = data;
        this.timeout = timeout;
        this.clientSocket = clientSocket;
        this.ip_addr = ip_addr;
        this.rport = rport;
        this.seq_no = seq_no;
        t = new Thread(this);
        t.start();
    }

    public void run() {
        try {
            while (true) {
              
                senddata(data);
                Thread.sleep(timeout);
            }
        }

        catch (InterruptedException e) {
            return;
        }
        catch (IOException e) {
        e.printStackTrace();
        }
    }

    public void senddata(byte[] data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip_addr, rport);
        clientSocket.send(sendPacket);
    }

    public boolean isAlive(){
        return t.isAlive();
    }

    public void interrupt(){
        t.interrupt();
    }
}

