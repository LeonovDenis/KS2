package ru.pelengator.API.driver.ethernet;


import java.io.OutputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPOutputStream extends OutputStream {

    public static final int DEFAULT_BUFFER_SIZE = 64000;
    public static final int DEFAULT_MAX_BUFFER_SIZE = 64010;

    protected DatagramSocket dsock = null;
    DatagramPacket dpack = null;
    InetAddress iAdd = null;
    int port = 0;

    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    byte[] outdata = null;
    int idx = 0; // buffer index; points to next empty buffer byte
    int bufferMax = DEFAULT_MAX_BUFFER_SIZE;

    /********************** constructors ********************/

    public UDPOutputStream() {
    }

    public UDPOutputStream(int buffSize) {
        setBufferSize(buffSize);
    }

    public UDPOutputStream(String address, int portI)
            throws UnknownHostException, SocketException, IOException {

        open(InetAddress.getByName(address), portI);
    }

    public UDPOutputStream(InetAddress address, int portI)
            throws SocketException, IOException {

        open(address, portI);
    }

    public UDPOutputStream(DatagramSocket dsock,InetAddress address,int portI) {
        this.dsock = dsock;
        this.iAdd = address;
        this.port = portI;
    }

    public UDPOutputStream(String address, int portI, int buffSize)
            throws UnknownHostException, SocketException, IOException {

        open(InetAddress.getByName(address), portI);
        setBufferSize(buffSize);
    }

    public UDPOutputStream(InetAddress address, int portI, int buffSize)
            throws SocketException, IOException {

        open(address, portI);
        setBufferSize(buffSize);
    }

    /************ opening and closing the stream ************/

    public void open(InetAddress address, int portI)
            throws SocketException, IOException {

        dsock = new DatagramSocket();
        iAdd = address;
        port = portI;
    }
    public void open(DatagramSocket ds,InetAddress address, int portI)
            throws SocketException, IOException {

        dsock =ds;
        iAdd = address;
        port = portI;
    }


    public void close() throws IOException {
        dsock.close();
        dsock = null;
        idx = 0;
    }

    /*********** writing to and flushing the buffer ************/

    public void flush() throws IOException {
        if (idx == 0) {  // no data in buffer
            return;
        }

        // copy what we have in the buffer so far into a new array;
        // if buffer is full, use it directly.
        if (idx == buffer.length) {
            outdata = buffer;
        } else {
            outdata = new byte[idx];
            System.arraycopy(buffer,
                    0,
                    outdata,
                    0,
                    idx);
        }

        // send data
        dpack = new DatagramPacket(outdata, idx, iAdd, port);
        dsock.send(dpack);

        // reset buffer index
        idx = 0;
    }

    /*
     *****************************************************************
     ***       Writes the input value to the UDP socket.  May      ***
     ***   buffer the value.                                       ***
     ***       Input value is converted to a byte.                 ***
     *****************************************************************
     */
    public void write(int value) throws IOException {
        buffer[idx] = (byte) (value & 0x0ff);
        idx++;

        if (idx >= buffer.length) {
            flush();
        }
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int off, int len) throws IOException {
        int lenRemaining = len;

        try {
            while (buffer.length - idx <= lenRemaining) {
                System.arraycopy(data,
                        off + (len - lenRemaining),
                        buffer,
                        idx,
                        buffer.length - idx);
                lenRemaining -= buffer.length - idx;
                idx = buffer.length;
                flush();
            }

            if (lenRemaining == 0) {
                return;
            }

            System.arraycopy(data,
                    off + (len - lenRemaining),
                    buffer,
                    idx,
                    lenRemaining);
            idx += lenRemaining;
        } catch (ArrayIndexOutOfBoundsException e) {
            // 04/03/02 UCB - DEBUG
            System.err.println("len: " + len);
            System.err.println("lenRemaining: " + lenRemaining);
            System.err.println("idx: " + idx);
            System.err.println("buffer.length: " + buffer.length);
            System.err.println("offset: " + off);
            System.err.println("data.length: " + data.length);
            throw e;
        }
    }

    /******************* buffer size accesors ******************/
    /*
     *****************************************************************
     ***       How many bytes are buffered before being flushed.   ***
     *****************************************************************
     */
    public int getBufferSize() {
        return buffer.length;
    }

    /*
     *****************************************************************
     ***       Allows user to set upper limit on output buffer     ***
     ***   size.  Set by default to DEFAULT_MAX_BUFFER_SIZE.       ***
     *****************************************************************
     */
    public void setMaxBufferSize(int max) {
        bufferMax = max;
    }

    /*
     *****************************************************************
     ***       Sets the length of the buffer.  Must be at least 1  ***
     ***   byte long.  Tries to flush any data currently in buffer ***
     ***   before resetting the size.                              ***
     *****************************************************************
     */
    public void setBufferSize(int buffSize) {
        try {
            flush();
        } catch (IOException ioe) {
        }

        if (buffSize == buffer.length) {
            // a no-op; we are already the right size
            return;
        } else if (buffSize > 0) {
            if (buffSize > bufferMax) {
                buffer = new byte[bufferMax];
            } else {
                buffer = new byte[buffSize];
            }
        } else {
            buffer = new byte[1];
        }
    }

    public void setiAdd(InetAddress iAdd) {
        this.iAdd = iAdd;
    }
}