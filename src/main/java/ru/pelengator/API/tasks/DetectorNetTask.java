package ru.pelengator.API.tasks;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;
import ru.pelengator.API.devises.china.ChinaDriverEthernet;
import ru.pelengator.API.driver.ethernet.EthernetDriver;

import java.net.InetAddress;


/**
 * Класс обертка задания
 */
public class DetectorNetTask extends DetectorTask {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorNetTask.class);
    private byte[] MSG = null;
    private InetAddress broadcastIp = null;
    private DetectorDriver driver = null;

    public DetectorNetTask(DetectorDriver driver, DetectorDevice device, byte[] MSG) {
        this(driver, device, MSG, null);

    }

    public DetectorNetTask(DetectorDriver driver, DetectorDevice device, byte[] MSG, InetAddress broadcastIp) {
        super(driver, device);
        this.MSG = MSG;
        this.broadcastIp = broadcastIp;
        this.driver = driver;
    }

    public byte[] sendCMD() {
        try {
            process();
        } catch (InterruptedException e) {
            return null;
        }
        return MSG;
    }

    @Override
    protected void handle() {


        ChinaDriverEthernet drv = (ChinaDriverEthernet) driver;

        EthernetDriver grabber = (EthernetDriver) drv.getGrabber();

        if (broadcastIp == null) {
            MSG = grabber.sendMSG(MSG);
        } else {
            MSG = grabber.sendMSG(MSG, broadcastIp,true);
        }

    }
}
