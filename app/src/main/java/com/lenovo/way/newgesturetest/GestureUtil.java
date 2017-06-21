package com.lenovo.way.newgesturetest;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author way
 * @data 2017/6/5
 * @description .
 */

public class GestureUtil {

    private DataReceive mDataReceive;

    private final String SOCKET_NAME = "gestured";
    private final String TAG = "LenovoPreviewApp";
    private String file = "/mnt/sdcard/Documents/GesturePreview/"; // image 保存路径

    private LocalSocket client;
    private LocalSocketAddress address;
    private ExecutorService mCachedThreadPool;
    private AcceptSocketThread mAcceptSocketThread;

    private int[] resolution = {224, 172};
    private int DepImgIndex = 0, IrImgIndex = 0;
    private int len = 172 * 224 * 2;
    private int connetTime = 1;
    private byte[] sendData;
    private byte[] byteString = new byte[4];
    private boolean isConnected = false;
    private boolean isSaveImage = false;
    private boolean delFile = false;

    private byte START_IMG = 0x02;
    private byte CLOSE_SOCKET = 0x03;
    private byte STOP_IMG = 0x04;
    private byte SET_SHORT_LONG_SWITCH = 0x05;
    private byte GET_SHORT_LONG_SWITCH = 0x06;
    private byte SET_FPS = 0x07;
    private byte GET_FPS = 0x08;
    private byte SET_EXP_TIME = 0x09;
    private byte GET_EXP_TIME = 0x0a;
    private byte START_INFO = 0x0b;
    private byte STOP_INFO = 0x0c;
    private byte GROUP_PREVIEW = 0x0e;
    private byte START_DUMP_FRAME = 0x12;
    private byte STOP_DUMP_FRAME = 0x13;

    // private MainActivity mMainActivity;

    public GestureUtil(){
        mCachedThreadPool = Executors.newCachedThreadPool();
    }


    public void useDataReceive(DataReceive dataReceive){
        this.mDataReceive = dataReceive;
    }

    public void setIsSaveImage(boolean isSaveImage){
        this.isSaveImage = isSaveImage;
    }

    public Boolean getIsSaveImage() {
        return isSaveImage;
    }

    // socketClient
    public void socketClient(){
        sendData = new byte[6];
        for (int i = 0; i < 6; i++) {
            sendData[i] = 0x00;
        }
        for (int i = 0; i < 4; i++) {
            byteString[i] = 0x00;
        }

        ConnectSocketThread connectSocketThread;

        if (client == null) {
            client = new LocalSocket();
            address = new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.RESERVED);
            connectSocketThread = new ConnectSocketThread();
            connectSocketThread.start();
        } else {
            address = new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.RESERVED);
            connectSocketThread = new ConnectSocketThread();
            connectSocketThread.start();
        }
    }

    public void startImg() {
        sendData[1] = START_IMG;
        sendMsg(sendData);
    }

    public void stopImg() {
        sendData[1] = STOP_IMG;
        sendMsg(sendData);
    }

    public void startInfo() {
        sendData[1] = START_INFO;
        sendMsg(sendData);
    }

    public void stopInfo() {
        sendData[1] = STOP_INFO;
        sendMsg(sendData);
    }

    public void setFps(int value) {
        sendData[1] = SET_FPS;
        addToDataByte(value);
        sendMsg(sendData);
    }

    public void setExp(int value) {
        sendData[1] = SET_EXP_TIME;
        addToDataByte(value);
        for (int i = 0; i < sendData.length; i++) {
            Log.d(TAG, "onClick-----setExp-----data[" + i + "]: " + sendData[i]);
        }
        sendMsg(sendData);
    }

    public void setRange(int value) {
        sendData[1] = SET_SHORT_LONG_SWITCH;
        addToDataByte(value);
        sendMsg(sendData);
    }

    public void getSetInfo() {
        sendData[1] = GET_FPS;  // 随便发送三个里面的一个值
        sendMsg(sendData);
    }

    public void startDump() {
        sendData[1] = START_DUMP_FRAME;
        sendMsg(sendData);
    }

    public void stopDump() {
        sendData[1] = STOP_DUMP_FRAME;
        sendMsg(sendData);
    }

    public void closePreview() {
        sendData[1] = CLOSE_SOCKET;
        sendMsg(sendData);
        isConnected = false;
        Log.d(TAG, "socket stop !!!");
        if (mAcceptSocketThread != null) {
            mAcceptSocketThread.stopThread();
        }

    }


    private class ConnectSocketThread extends Thread {

        @Override
        public void run() {

            while (!isConnected && null != address && connetTime <= 10) {
                try {
                    sleep(1000);
                    client.connect(address);
                    isConnected = true;
                    Log.i(TAG, "connectSocketThread------Try to connect socket");
                } catch (Exception e) {
                    connetTime++;
                    isConnected = false;
                    Log.i(TAG, "connectSocketThread------Connect fail" + e.toString());
                }
            }

            if (isConnected) {
                mAcceptSocketThread = new AcceptSocketThread();
                // 防止底层收不到GROUP_PREVIEW命令 延时100ms
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        mAcceptSocketThread.start();
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, 100);
            }
        }
    }

    private class AcceptSocketThread extends Thread {
        private boolean start = true;

        public void stopThread() {
            start = false;
        }

        @Override
        public void run() {
            while (client != null && client.isConnected()) {
                Log.d(TAG, "AcceptSocketThread-----isConnected!");

                sendData[1] = GROUP_PREVIEW;
                sendMsg(sendData);

                int imgLen = len / 2;
                byte[] accept = new byte[len];
                int read = 0;

                try {
                    InputStream inputStream = client.getInputStream();
                    int[] imageDatas = new int[172 * 224];

                    int gesture_nums = 0;
                    int fingercnt = 0;
                    int eventCode = 0;
                    int fps = 0;

                    int set_fps = 0, set_exp = 0, set_range = 0;

                    int expectedSize = 172 * 224 * 2 + 4;
                    byte[] data = new byte[expectedSize];

                    while (start && read != -1) {

                        int totalRead = 0;
                        int[] header = {0, 0};
                        int[] h = {0, 0};

                        while (totalRead < expectedSize) {
                            read = inputStream.read(data, 0, expectedSize - totalRead);
                            // Log.d(TAG, "AcceptSocketThread-----receive data len: " + read);

                            if (read == -1) {
                                //  throw new IOException("Not enough data in stream");
                                break;
                            } else if (read == 12) { // post_fingerinfo

                                h[0] = (data[0] & 0xFF) | (data[1] & 0xFF) << 8;
                                h[1] = (data[2] & 0xFF) | (data[3] & 0xFF) << 8;

                                if (h[0] == 0xAB && h[1] == 0xCD) {
                                    eventCode = (data[4] & 0xFF) | (data[5] & 0xFF) << 8;
                                    gesture_nums = (data[6] & 0xFF) | (data[7] & 0xFF) << 8;
                                    fingercnt = (data[8] & 0xFF) | (data[9] & 0xFF) << 8;
                                    fps = (data[10] & 0xFF) | (data[11] & 0xFF) << 8;
                                    Log.d(TAG, "post_fingerinfo-----longshort: " + gesture_nums + " , fingercnt: " + fingercnt + " , eventCode: " + eventCode);
//                                    mMainActivity.setGestureInfo(fps, 0, gesture_nums, fingercnt, eventCode);
                                    mDataReceive.setGestureInfo(fps, 0, gesture_nums, fingercnt, eventCode);
                                }
                                continue;

                            } else if (read == 10) { // post_parameters

                                h[0] = (data[0] & 0xFF) | (data[1] & 0xFF) << 8;
                                h[1] = (data[2] & 0xFF) | (data[3] & 0xFF) << 8;

                                if (h[0] == 0xAA && h[1] == 0xCC) {
                                    set_fps = (data[4] & 0xFF) | (data[5] & 0xFF) << 8;
                                    set_exp = (data[6] & 0xFF) | (data[7] & 0xFF) << 8;
                                    set_range = (data[8] & 0xFF) | (data[9] & 0xFF) << 8;
                                    Log.d(TAG, "post_parameters-----set_fps: " + set_fps + " , set_exp: " + set_exp + " , set_range: " + set_range);
                                    mDataReceive.setSetInfo(set_fps, set_exp, set_range);
                                }
                                continue;

                            } else {

                                if (totalRead == 0) {
                                    header[0] = (data[0] & 0xFF) | (data[1] & 0xFF) << 8;
                                    header[1] = (data[2] & 0xFF) | (data[3] & 0xFF) << 8;

                                    if (header[0] == 0xAB && header[1] == 0xAB) {
                                        // Log.d(TAG, "AcceptSocketThread----this package is right-------Depth");
                                        System.arraycopy(data, 4, accept, totalRead, read - 4);
                                        totalRead += read;

                                    } else if (header[0] == 0xBA && header[1] == 0xBA) {
                                        // Log.d(TAG, "AcceptSocketThread----this package is right-------Ir");
                                        System.arraycopy(data, 4, accept, totalRead, read - 4);
                                        totalRead += read;

                                    } else {
                                        Log.d(TAG, "AcceptSocketThread----head var:"
                                                + header[0] + " ### " + header[1] + " skip this package");
                                        continue;
                                    }
                                } else {
                                    System.arraycopy(data, 0, accept, totalRead - 4, read);
                                    totalRead += read;
                                }
                            }
                        }

                        // Log.d(TAG, "set depth image of totalRead: "+ accept.length + " image length: " + imageDatas.length);

                        if (header[0] == 0xAB && header[1] == 0xAB) {
                            for (int i = 0; i < imgLen; i++) {
                                int i1 = (accept[i * 2] & 0xFF) | (accept[i * 2 + 1] & 0xFF) << 8;
                                i1 = i1 >> 2;
                                i1 = i1 | i1 << 8 | i1 << 16 | 255 << 24;
                                imageDatas[i] = i1;
                            }
                            mDataReceive.setDepthImageData(imageDatas);

                        } else if (header[0] == 0xBA && header[1] == 0xBA) {
                            for (int i = 0; i < imgLen; i++) {
                                short i1 = (short) ((accept[i * 2] & 0xFF) | (accept[i * 2 + 1] & 0xFF) << 8);
                                i1 = (short) (i1 >> 2);
                                i1 += 30;
                                if (i1 < 0) {
                                    i1 = 0;
                                } else if (i1 > 255) {
                                    i1 = 255;
                                }
                                int i2 = i1 | i1 << 8 | i1 << 16 | 255 << 24;
                                imageDatas[i] = i2;
                            }
                            mDataReceive.setIRImageData(imageDatas);
                        }

                        if (getIsSaveImage()) {
                            SaveImageRun saveImageRun = null;
                            if (header[0] == 0xAB && header[1] == 0xAB) {
                                saveImageRun = new SaveImageRun(accept, 0);
                            } else if (header[0] == 0xBA && header[1] == 0xBA) {
                                saveImageRun = new SaveImageRun(accept, 1);
                            }
                            mCachedThreadPool.execute(saveImageRun);
                        }
                    }
                    Log.d(TAG, "AcceptSocketThread-----socket close");
                    isConnected = false;
                    inputStream.close();
                    client.close();

                    client = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "AcceptSocketThread-----accept exception" + e.toString());
                }
            }
        }
    }


    public void sendMsg(byte[] data) {
        if (client == null || !client.isConnected()) {
            Log.d(TAG, "The socket is not connected!!!");
            return;
        }
        try {
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            out.write(data);
            Log.d(TAG, "The socket is send signal and data is:!" + data[1]);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "The socket is not send well !!!" + e.toString());
        }
    }

    // 添加到sendData最后四位---> 2345位
    private void addToDataByte(int res) {
        sendData[2] = (byte) (res & 0xff);
        sendData[3] = (byte) ((res >> 8) & 0xff);
        sendData[4] = (byte) ((res >> 16) & 0xff);
        sendData[5] = (byte) (res >>> 24);
    }



    public class SaveImageRun implements Runnable {
        private byte[] images = new byte[len];
        private String fileName;

        public SaveImageRun(byte[] accept) {
            synchronized (MainActivity.class) {
                fileName = file + DepImgIndex + ".raw";
                DepImgIndex++;
                System.arraycopy(accept, 0, images, 0, len);
            }
        }

        public SaveImageRun(byte[] accept, int a) {
            synchronized (MainActivity.class) {
                if (a == 0) {
                    fileName = file + "Depth_" + DepImgIndex + ".raw";
                    DepImgIndex++;
                    System.arraycopy(accept, 0, images, 0, len);
                } else if (a == 1) {
                    fileName = file + "Ir_" + IrImgIndex + ".raw";
                    IrImgIndex++;
                    System.arraycopy(accept, 0, images, 0, len);
                }
            }
        }

        @Override
        public void run() {
            FileInputStream is = null;
            try {
                File sd = Environment.getExternalStorageDirectory();
                File file = new File(fileName);
                FileOutputStream os = new FileOutputStream(file);
                os.write(images, 0, len);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
