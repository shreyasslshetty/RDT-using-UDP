
import socket
import random
import threading
import sys
from time import sleep, time
 
chunk_size = 512
header_size = 2
loop_ip = "127.0.0.1"
new_file = "File_Received.png"

def send_ACK(sock, addr, packet_no):
    sock.sendto(packet_no.to_bytes(header_size, byteorder="big"), addr)
    
class delay_time_ACK(threading.Thread):
    def __init__(self, sock, addr, packet_no, max_delay_time):
        super().__init__()
        
        self.sock = sock
        self.addr = addr
        self.packet_no = packet_no
        self.max_delay_time = max_delay_time
        
    def run(self):
        delay_time = random.random() * self.max_delay_time / 1000
        sleep(delay_time)
        
        try:
            send_ACK(self.sock, self.addr, self.packet_no)
        except OSError:
            pass


port = int(sys.argv[1])
N = int(sys.argv[2])
loss_prob = float(sys.argv[3])
delay_time = int(sys.argv[4]) 

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((loop_ip, port))

received_numbers = set()
buf = {} 
rcv_base = 1

sender_addr = None
while True:
    
    if sender_addr is None:
        packet, sender_addr = sock.recvfrom(chunk_size)
        start_time = time()
    else:
        packet = sock.recv(chunk_size)
        
    packet_no = int.from_bytes(packet[:header_size], byteorder="big")
    data_bytes = packet[header_size:chunk_size]
    
    if packet_no == 0:
        end_time = time()
        break
    
    else:
        
        if random.random() > loss_prob:
            ack_thread = delay_time_ACK(sock, sender_addr, packet_no, delay_time)
                
            if packet_no in range(rcv_base, rcv_base + N):
                ack_thread.start()
                
                if packet_no not in received_numbers:
                    buf[packet_no] = data_bytes
                    received_numbers.add(packet_no)
                    
                    while rcv_base in received_numbers:
                        rcv_base += 1
                    received_numbers -= set(range(rcv_base))
                        
            elif packet_no in range(rcv_base - N, rcv_base):
                ack_thread.start()
            
sock.close()


print("\n")
print("Receiving file from Client..... \n")

sleep(3)

print("Writing file to Server directory. Please wait... \n")
buf_concat = bytes()
for packet_no in sorted(buf):
    buf_concat += buf[packet_no]
    
with open(new_file, "wb") as f:
    f.write(buf_concat)

sleep(3)

print("Completed..\n")

sleep(0.5)

print("File Transfer Time:", float(end_time - start_time),"sec")

print("\n")


    
