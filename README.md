# RDT-using-UDP
This is a program developed using Java where we conduct reliable data transfer (RDT 3.0) using User Data Protocol (UDP)

# DESCRIPTION

# RELIABLE DATA TRANSFER USING UDP

The Internet network layer provides only best effort service with no guarantee that packets arrive at their destination. Also, since each packet is routed individually it is possible that packets are received out of order. For this project we are using a connection-less service which is provided by UDP (User-Datagram Protocol) which is in the Transport Layer of OSI model. It is necessary to have a reliable data transfer (RDT) protocol to ensure delivery of all packets and to enable the receiver to deliver the packets in order to its application layer.

# Program Components

# Rdtu-server
- This is the first program to start with as this is the server and it senses its port number assigned to know whether any packet or data is being transmitted.
- The run command within the program which checks for delays.
- The IP address is set to 127.0.0.1 which is the loop back address as the program is running on the same system.
- The chunk size is set to 512 byets.
- Once, the data is being received from the client using the same port number then the chucks are stored within the server directory and the estimated time is calculated on the difference of end time and start time.
- The server also does delay time checks for the ACK sent for each chuck received.

# Rdtu-client
- This program splits the files into chunks.
- Then the connection is established with respect to the server port number.
- The file is split into chunks of big-endian format.
- Once, the transfer is done, the connection is stopped.
- The program also keeps a copy of the ACK for the checking the packet sent or not.

# Rdtu-proxy
- The proxy is the middleware layer between the server and client and checks whether the connection is alive or not.
- The proxy takes chunks from the client program and sends it to the server.
