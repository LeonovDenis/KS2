package ru.pelengator.API.driver.ethernet;


import java.io.InputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPInputStream extends InputStream {

    public static final int PACKET_BUFFER_SIZE = 64000;
    public int SO_TIME_OUT = 0;

    public InetAddress clientIP;
    public int clientPort;
    public DatagramSocket dsock = null;
    public DatagramPacket dpack = null;

    public  byte[] ddata = new byte[PACKET_BUFFER_SIZE];
    public int packSize = 0;
    public  int packIdx = 0;

    public int value;

    /********************** constructors ********************/

    public UDPInputStream() {
    }


    public UDPInputStream(InetAddress address, int port, int SO_TIME_OUT)
            throws UnknownHostException, SocketException {
        open(address, port, SO_TIME_OUT);
    }

    /************ opening and closing the stream ************/
    public void open(InetAddress address, int port, int SO_TIME_OUT)
            throws UnknownHostException, SocketException {

        dsock = new DatagramSocket(port, address);
        this.SO_TIME_OUT = SO_TIME_OUT;
    }


    public void close() throws IOException {
        dsock.close();
        dsock = null;
        ddata = null;
        packSize = 0;
        packIdx = 0;
        this.SO_TIME_OUT = 0;
    }

    /****** reading, skipping and checking available data ******/
    /*
     *****************************************************************
     ***       Determines how many more values may be read before  ***
     ***   next blocking read.                                     ***
     *****************************************************************
     */
    public int available() throws IOException {
        return packSize - packIdx;
    }

    /*
     *****************************************************************
     ***       Reads the next value available.  Returns the value  ***
     ***   as an integer from 0 to 255.                            ***
     *****************************************************************
     */
    public int read() throws IOException {
        if (packIdx == packSize) {
            receive();
        }

        value = ddata[packIdx] & 0xff;
        packIdx++;
        return value;
    }

    /*
     *****************************************************************
     ***   Reads the next buff.length values into the input        ***
     ***   byte array, buff.                                       ***
     *****************************************************************
     */
    public int read(byte[] buff) throws IOException {
        return read(buff, 0, buff.length);
    }

    /*
     *****************************************************************
     ***       Reads the next len values into the input byte array,***
     ***   buff, starting at offset off.                           ***
     *****************************************************************
     */
    public int read(byte[] buff, int off, int len) throws IOException {
        if (packIdx == packSize) {
            receive();
        }

        int lenRemaining = len;//сколько прочитать

        while (available() < lenRemaining) {
            System.arraycopy(ddata,
                    packIdx,
                    buff,
                    off + (len - lenRemaining),
                    available());
            lenRemaining -= available();
            receive();
        }

        System.arraycopy(ddata,
                packIdx,
                buff,
                off + (len - lenRemaining),
                lenRemaining);
        packIdx += lenRemaining;
        return len;
    }

    /*
     *****************************************************************
     ***       Skips over the next len values.                     ***
     *****************************************************************
     */
    public long skip(long len) throws IOException {
        if (packIdx == packSize) {
            receive();
        }

        long lenRemaining = len;

        while (available() < lenRemaining) {
            lenRemaining -= available();
            receive();
        }

        packIdx += (int) lenRemaining;
        return len;
    }

    /****************** receiving more data ******************/
    /*
     *****************************************************************
     ***       A blocking read to receive more data from the UDP   ***
     ***   socket.                                                 ***
     *****************************************************************
     */
    public void receive() throws IOException {
        dpack = new DatagramPacket(ddata, PACKET_BUFFER_SIZE);
        if (SO_TIME_OUT > 0) {
            dsock.setSoTimeout(SO_TIME_OUT);
        }
        dsock.receive(dpack);
        packIdx = 0;
        packSize = dpack.getLength();
        this.clientIP = dpack.getAddress();
        this.clientPort = dpack.getPort();

    }

    /********* marking and reseting are unsupported ********/
    public void mark(int readlimit) {
    }

    public void reset() throws IOException {
        throw new IOException("Marks are not supported by UDPInputStream.");
    }

    public boolean markSupported() {
        return false;
    }

    public InetAddress getClientIP() {
        return clientIP;
    }

    public int getClientPort() {
        return clientPort;
    }

    public DatagramSocket getDsock() {
        return dsock;
    }
}
