/**
 * FastFTP Class
 * FastFtp implements a basic FTP application based on UDP data transmission and
 * alternating-bit stop-and-wait concept
 * @author      Marcus Tang 10086730
 * @version     1.0, 1 Feb 2017
 1. Read bytes from file chunk by chunk
 2. Create new segment object using each chunk
 3. Get bytes from segment object using getBytes() method
 4. Create a datagram pakcet object using bytes you got from step 3
 5. Send data to other side
  */
import java.io.*;
import java.net.*;
import java.util.*;



public class FTPClient {

    /**
     * Constructor to initialize the program
     *
     * @param serverName	server name
     * @param server_port	server port
     * @param file_name		name of file to transfer
     * @param timeout		Time out value (in milli-seconds).
     * @param
     */
	 String server; //server name
	 String file; // file name
	 int to; // timeout period
   int port; // port number
   int seqNum; // sequence number
	 Socket socket; // socket connection to connect to server



	public FTPClient(String server_name, int server_port, String file_name, int timeout) {

	/* Initialize values */
		this.server = server_name;
		this.port = server_port;
		this.file = file_name;
    this.to = timeout;
	}


    /**
     * Send file content as Segments
     *
     */
	public void send() {

	try{
		//connects to port server listening at port given in argument
		socket = new Socket (server, port);

		//FileOutputStream Fout = new FileOutputStream(file);
		// create data output stream
		DataOutputStream Dout = new DataOutputStream(socket.getOutputStream());

		//Client sends file name to the server
		Dout.writeUTF(file);


        // Creates data input stream and reads a byte from the file
        DataInputStream Dins = new DataInputStream(socket.getInputStream());
        byte response = Dins.readByte();
        // Creates offsets to gather 1000 byte chunks at a time
        int endOff = 1000;
        int startOff = 0;
        // Checks to see if the server is ready to read bytes from client
	      if(response == 0){
             // creates with the string name given as an argument
			       File transfer = new File(file);
             /* Creates a segment package for each byte to prepared for server
             to receive*/
             Segment seg = new Segment ();
             // Creates an array with length of file to store bytes from file
			       byte[] chunks = new byte[(int) transfer.length()];
             // Creates UDP connection
			       DatagramSocket clientSocket = new DatagramSocket(5555);
			       InetAddress IPAddress = InetAddress.getByName("localhost");
		   	     FileInputStream Fin = new FileInputStream(transfer);
             /* Gets the length of the file to check if a thousand bytes can be
             taken out of the array. */
		  	     int length = Fin.read(chunks, 0, chunks.length);
		  	     Fin.close();
             /* Taking out a thousand bytes from the array until there is less
             than a thousand bytes left in the file to read */
             while(length > 1000){

				        byte[] maxPay = Arrays.copyOfRange(chunks, startOff, endOff);
                seg.setPayload(maxPay);
                seg.setSeqNum(seqNum);

			    	    packetSent(seg, IPAddress, port, clientSocket, length);
                startOff = startOff + seg.MAX_PAYLOAD_SIZE;
                endOff = endOff + seg.MAX_PAYLOAD_SIZE;
                length = length - 1000;
			      }

              byte[] smallArry = Arrays.copyOfRange(chunks, 0, length);
              seg.setPayload(smallArry);
              seg.setSeqNum(seqNum);
              packetSent(seg, IPAddress, port, clientSocket, length);


         }else{
           System.out.println("Server is not ready");
         }
         // Closing off all streams
         Dout.writeUTF(file);
         Dout.close();
         Dins.close();
         socket.close();


	}catch (IOException e){
	   System.out.println(e);
	}

}
// Method that sends apackets and checks to see if they need to be reset
public void packetSent (Segment pack, InetAddress address, int portNum, DatagramSocket sock, int payLength){

try{
     DatagramPacket sendPacket = new DatagramPacket(pack.getBytes(), pack.getLength(), address, portNum);
     sock.send(sendPacket);
     sock.setSoTimeout(to);
     byte [] recArry = new byte[payLength];
     DatagramPacket receivePacket = new DatagramPacket(recArry, payLength);

     try{
        sock.receive(receivePacket);
        seqNum = (seqNum + 1) % 2;


     }catch(SocketTimeoutException e){
        // Resends packet if the timer runs out
        System.out.println("Timeout reached, resending packet...");
        packetSent(pack, address, portNum, sock, payLength);

     }
   }catch(IOException e){
     System.out.println("error");
     //seqNum = (seqNum - 1) % 2; //might not need to reset seqNum
     //packetSent(pack, address, portNum, sock, payLength);
   }


}

     	// A simple test driver

	public static void main(String[] args) {

		String server = "localhost";
		String file_name = "";
		int server_port = 8888;
                int timeout = 50; // milli-seconds (this value should not be changed)


		// check for command line arguments
		if (args.length == 3) {
			// either provide 3 parameters
			server = args[0];
			server_port = Integer.parseInt(args[1]);
			file_name = args[2];
		}
		else {
			System.out.println("wrong number of arguments, try again.");
			System.out.println("usage: java FTPClient server port file");
			System.exit(0);
		}


		FTPClient ftp = new FTPClient(server, server_port, file_name, timeout);

		System.out.printf("sending file \'%s\' to server...\n", file_name);
		ftp.send();
		System.out.println("file transfer completed.");
	}

}
