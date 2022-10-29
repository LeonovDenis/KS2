package ru.pelengator.API.driver.ethernet;


import at.favre.lib.bytes.Bytes;

import java.io.IOException;
import java.net.InetAddress;

public class TestInputStream extends UDPInputStream {
    private static short frameID = 0;
    private static short fragID = 0;

    @Override
    public void receive() throws IOException {

        Bytes empty = Bytes.empty();
        Bytes temp = empty.append((byte) 0xA2).append((byte) 0x05)
                .append(frameID).append(fragID)
                .append((short) 128).append((short) 128).append((byte) 0x01).append((byte) 0x02).append(128 * 128 * 2);

        Bytes telo = Bytes.allocate(1024, (byte) 0x1B);
        Bytes append = temp.append(telo);
        ddata = append.array();

        packSize = append.length();

        packIdx = 0;
        clientIP = InetAddress.getByName("172.0.0.1");
        clientPort = 55555;
        fragID++;

        if(fragID==32){
            frameID++;
            fragID=0;
        }
    }
}
