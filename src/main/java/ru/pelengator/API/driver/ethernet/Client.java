package ru.pelengator.API.driver.ethernet;

import ru.pelengator.API.driver.NetworkInfo;

import java.io.IOException;
import java.net.*;

public class Client {


    private InetAddress ip;
    private int port;
    private DatagramSocket ds;

    public Client(String ip, int port, NetworkInfo selNetworkInterface) throws UnknownHostException, SocketException {
        this.ip = InetAddress.getByName(ip);
        this.port = port;
        NetworkInterface byName = NetworkInterface.getByName(selNetworkInterface.getName());

        try {
            this.ds = new DatagramSocket(port, byName.getInetAddresses().nextElement());
        } catch (SocketException e) {
            System.err.println("Сокет занят на клиенте");
        }


    }

    public Client(String ip, int port, NetworkInfo selNetworkInterface, DatagramSocket ds) throws UnknownHostException, SocketException {
        this.ip = InetAddress.getByName(ip);
        this.port = port;
        NetworkInterface byName = NetworkInterface.getByName(selNetworkInterface.getName());


        this.ds = ds;
    }


    public boolean sendMsg(byte[] txt) throws IOException {
        DatagramPacket DpSend = new DatagramPacket(txt, txt.length, ip, port);
        try {
            ds.send(DpSend);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void stop() {
        ds.close();
    }
}
