package ru.pelengator.model;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.List;

/**
 * Класс для списка сетевых интерфейсов.
 */
public class NetworkInfo {
    /**
     * Адрес адаптера.
     */
    InetAddress address;
    /**
     * Бродкаст адрес.
     */
    InetAddress broadcast;

    /**
     * Наименование.
     */
    private String name;
    /**
     * Наименование,отображаемое.
     */
    private String displayName;
    /**
     * Индекс детектора в списке.
     */
    private int index;
    /**
     * Интерфейс сетевой.
     */
    private NetworkInterface networkInterface;

    /**
     * Конструктор для USB
     * @param index
     */
    public NetworkInfo(int index) {
        this.name = "USB 3.0";
        this.index = index;
        this.networkInterface = null;
        this.displayName = "USB 3.0";
        this.address = null;
        this.broadcast = null;

    }

    /**
     * Конструктор для ethernet
     * @param name наименование
     * @param networkInterface интерфейс
     * @param index
     */
    public NetworkInfo(String name, NetworkInterface networkInterface, int index) {
        this.name = name;
        this.index = index;
        this.networkInterface = networkInterface;
        this.displayName = networkInterface.getDisplayName();
        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
        this.address = interfaceAddresses.get(0).getAddress();
        this.broadcast = interfaceAddresses.get(0).getBroadcast();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        if (address == null) {
            return displayName;
        } else {
            String name = address.toString();
            String modyName = name.substring(1, name.length());
            String fullName = "IP: " + modyName;
            return fullName ;
        }
    }

    public InetAddress getBroadcast() {
        return broadcast;
    }

    public InetAddress getAddress() {
        return address;
    }
}