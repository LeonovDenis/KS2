package ru.pelengator.API.driver.ethernet;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public class NetUtils {
    /**
     * Получение списка широковещательных адресов
     *
     * @return
     * @throws SocketException
     */
    public static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(a -> a.getBroadcast())
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }

    /**
     * Список интерфейсов
     *
     * @return
     * @throws SocketException
     */
    public static List<NetworkInterface> findInterfaces() {
        List<NetworkInterface> interfaceList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces
                    = null;
            interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                interfaceList.add(networkInterface);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return interfaceList;
    }
}
