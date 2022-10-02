package ru.pelengator.API.driver.ethernet;


import ru.pelengator.model.NetworkInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;

public class Server {

    private int port;
    private int buff;

    private byte receiveData[] = null;
    private DatagramSocket serverSocket;
    private DatagramPacket receivePacket;

    public Server(int port, int buff, NetworkInfo selNetworkInterface) throws IOException {
        this.port = port;
        this.buff = buff;
        NetworkInterface byName = NetworkInterface.getByName(selNetworkInterface.getName());
        try {
            serverSocket = new DatagramSocket(port, byName.getInetAddresses().nextElement());
        }catch (SocketException e) {
            serverSocket = new DatagramSocket(port+1, byName.getInetAddresses().nextElement());
        }
        receiveData = new byte[buff];//размер получаемой посылки
        receivePacket = new DatagramPacket(receiveData, receiveData.length);//создание пакета
    }

    public byte[] listen() {
        byte[] data = null;
        try {
            serverSocket.receive(receivePacket);//ожидание данных
            data = Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength());//полученная строка

        } catch (IOException e) {
            System.out.println("Закрытие сервера");
            return null;
        }
        return data;
    }

    public void stop() {
        serverSocket.close();
    }

    public DatagramSocket getSS() {
        return serverSocket;
    }

}
