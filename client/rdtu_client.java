import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class rdtu_client {
   
    private static final int chunk_size = 512;
    private static final int header_size = 2; 
    private static final int ack = 2; 

    private static final int sent_val = 0;
    private static final int ack_val = 1;
    private static final int use = 2;

    private static String file_loc;
    private static int rport;
    private static int win_size;
    private static int rt_timeout;
    private static String loop_ip;
    private static File f;
    private static DatagramSocket client_soc;
    private static InetAddress loop_ip_addr;

    public static void main(String args[]) {
       
        file_loc = args[0];
        rport = Integer.parseInt(args[1]);
        win_size = Integer.parseInt(args[2]);
        rt_timeout = Integer.parseInt(args[3]);
        loop_ip = "127.0.0.1"; 
       
        f = new File(file_loc);
       
        List<byte[]> segments;

        try{
          
            segments = split_file(f);

            start_con();

            send_packet(segments);

            stop_con();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void send_packet(List<byte[]> segments) throws IOException {
        int send_base = 1; 
        int next_seq = send_base; 

        int no_of_packets = segments.size();
        int no_of_received_ack = 0;

        rdtu_proxy[] threads = new rdtu_proxy[win_size];
        int[] ack_buffer = new int[win_size];
        for(int i = 0; i <win_size ; i++)
        {
            ack_buffer[i] = use;
        }

        while (no_of_received_ack < no_of_packets)
        {
          
            while( next_seq >= send_base && next_seq < send_base + win_size && next_seq <= no_of_packets)
            {
                int thread_index = (next_seq-1) % win_size;

                if(ack_buffer[thread_index] == use)
                {
                    byte[] segment = segments.get(next_seq-1);
                    threads[thread_index] = new rdtu_proxy(segment,rt_timeout,client_soc,loop_ip_addr,rport,next_seq);
                    ack_buffer[thread_index] = sent_val;
                    next_seq++;
                }
            }

            int ack_sequence_no = check_res();

            if(ack_sequence_no >= send_base && ack_sequence_no < send_base + win_size )
            {
                int thread_index = (ack_sequence_no-1) % win_size;
                rdtu_proxy ack_val_thread = threads[thread_index];

                if(ack_val_thread.isAlive())
                {
                    ack_val_thread.interrupt();
                    no_of_received_ack++;
                    ack_buffer[thread_index] = ack_val;

                    if(ack_sequence_no == send_base)
                    {
                        ack_buffer[(send_base-1)%win_size] = use;
                        send_base++;

                        while(ack_buffer[(send_base-1)%win_size] == ack_val)
                        {
                            ack_buffer[(send_base-1)%win_size] = use;
                            send_base++;
                        }

                    }
                }
            }
        }
    }

    public static List<byte[]> split_file(File f) throws IOException {
        int sequence_no = 1;
        byte[] file_content = Files.readAllBytes(f.toPath());
        int file_size = file_content.length;
        int segment_count = (int)(Math.ceil((double)file_size / (double)(chunk_size-header_size)));

        List<byte[]> segments = new ArrayList<byte[]>();

        int start_index = 0;
        int end_index = chunk_size - header_size;

        while (sequence_no < segment_count)
        {
            byte[] segment = new byte[chunk_size];

            ByteBuffer b = ByteBuffer.allocate(4);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putInt(sequence_no);
            byte[] big_endian = b.array();
            segment[0] = big_endian[2];
            segment[1] = big_endian[3];

            byte[] slice = Arrays.copyOfRange(file_content,start_index,end_index);
            for(int i=0; i<chunk_size-header_size;i++)
            {
                segment[i+header_size] = slice[i];
            }

            segments.add(segment);
            sequence_no++;

            start_index = end_index ;
            end_index = start_index + chunk_size-header_size;
        }

        end_index = file_size;
        byte[] segment = new byte[end_index-start_index+header_size];

        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(sequence_no);
        byte[] big_endian = b.array();
        segment[0] = big_endian[2];
        segment[1] = big_endian[3];

        byte[] slice = Arrays.copyOfRange(file_content,start_index,end_index);
        for(int i=0; i<slice.length; i++)
        {
            segment[i+header_size] = slice[i];
        }
        segments.add(segment);
        return segments;
    }

    public static void start_con() throws IOException {
        client_soc =  new DatagramSocket();
        loop_ip_addr = InetAddress.getByName(loop_ip);
    }

    public static void send_chunk(byte[] segment) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(segment, segment.length, loop_ip_addr, rport);
        client_soc.send(sendPacket);
    }

    public static int check_res() throws IOException {
        byte[] receiveData = new byte[ack];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        client_soc.receive(receivePacket);
        byte[] data = receivePacket.getData();
        byte[] big_endian = new byte[4];
        big_endian[0] = 0;
        big_endian[1] = 0;
        big_endian[2] = data[0];
        big_endian[3] = data[1];
        int ack_seq_no = java.nio.ByteBuffer.wrap(big_endian).getInt();
        return ack_seq_no;
    }


    public static  void stop_con() throws IOException {
        byte[] head = {0,0}; 
        DatagramPacket sendPacket = new DatagramPacket(head, 2, loop_ip_addr, rport);
        client_soc.send(sendPacket);
        client_soc.close();
    }
}