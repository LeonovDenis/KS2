package ru.pelengator.API.driver;


import at.favre.lib.bytes.Bytes;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Driver {

    AtomicBoolean validHendler = new AtomicBoolean(false);
    AtomicBoolean isOpened = new AtomicBoolean(false);
    AtomicBoolean flag_device_connected = new AtomicBoolean(false);
    Dimension size = null;

    List<DetectorDevice> getDDevices(List<DetectorDevice> devices);

    ByteBuffer getImage();

    int getWidth();

    int getHeight();

    FT_STATUS create();

    FT_STATUS close();

    void stopSession();

    FT_STATUS startSession();

    FT_STATUS setDimension(boolean set);

    FT_STATUS setCapacity(boolean set);

    FT_STATUS setPower(boolean set);


    FT_STATUS setIntTime(int time);

    FT_STATUS setVR0(int value);

    FT_STATUS setVVA(int value);

    FT_STATUS setREF(int value);

    FT_STATUS setID();

    FT_STATUS setID(byte[] data, int size, boolean startPKG);

    FT_STATUS setSpecPower(int vR0,int rEF,int rEF1,int vOS);

    Bytes nextFrame();

    void clearBuffer();

    boolean isOnline();
}
