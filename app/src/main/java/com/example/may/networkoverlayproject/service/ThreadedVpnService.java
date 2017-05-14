package com.example.may.networkoverlayproject.service;

import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;


public class ThreadedVpnService extends VpnService {
    public ThreadedVpnService() {
    }

    private Thread readThread, writeThread;
    private ParcelFileDescriptor vpnInterface;
    Builder builder = new Builder();
    private String REMOTE_ADDR = "104.154.153.166";
    private int REMOTE_PORT = 8888;
    static final long[] incount = {0};
    static final long[] outcount = {0};
    IBinder mBinder = new LocalBinder();
    int retryCount = 0;
    final int MAX_RETRY_COUNT = 3;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (retryCount < MAX_RETRY_COUNT) {
                    try {
                        vpnInterface = builder.setSession("MyVPNService")
                                .addAddress("192.168.0.1", 24)
                                .addDnsServer("8.8.8.8")
                                .addRoute("0.0.0.0", 0).establish();
                        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

                        Socket tunnel = new Socket();
                        tunnel.connect(new InetSocketAddress(REMOTE_ADDR, REMOTE_PORT));
                        protect(tunnel);

                        Log.d("Connected", tunnel.toString());

                        DataInputStream dataInputStream = new DataInputStream(tunnel.getInputStream());
                        DataOutputStream dataOutputStream = new DataOutputStream(tunnel.getOutputStream());

                        writeThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ByteBuffer packet = ByteBuffer.allocate(64000);
                                while (!Thread.interrupted()) {
                                    packet.clear();
                                    try {
                                        int length = in.read(packet.array());
                                        if (length > 0) {
                                            Log.d("OUTGOING", "input length > 0");

                                            packet.limit(length);
                                            byte[] remaining = new byte[packet.remaining()];
                                            packet.get(remaining, 0, remaining.length);

                                            Log.d("OUTGOING", "writing to dataoutputstream");

                                            dataOutputStream.writeShort(length);
                                            dataOutputStream.writeShort(0);
                                            dataOutputStream.write(remaining);
                                            outcount[0]++;

                                            Log.d("OUTGOING", "done writing to dataoutputstream");

                                        }

                                    } catch (SocketException se) {
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                        writeThread.start();

                        while (!Thread.interrupted()) {
                            int length = dataInputStream.readShort();
                            dataInputStream.readShort();
                            byte[] data = new byte[length];

                            Log.d("INCOMING", "read length etc");
                            Log.d("INCOMING", "reading data");

                            dataInputStream.read(data);
                            Log.d("INCOMING", "writing to vpn out");

                            out.write(data);
                            incount[0]++;
                            Log.d("INCOMING", "done writing");
                        }
                        writeThread.interrupt();

                    } catch (SocketException e) {
                        retryCount++;
                        continue;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (vpnInterface != null) {
                                vpnInterface.close();
                                vpnInterface = null;
                            }
                        } catch (Exception e) {

                        }
                        break;
                    }
                }
            }

        }, "MyVpnRunnable");

        readThread.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onDestroy() {
        if (readThread != null) {
            readThread.interrupt();
        }
        if(writeThread != null){
            writeThread.interrupt();
        }
        super.onDestroy();
    }

    public void stopVpn(){
        try {
            vpnInterface.close();
            readThread.interrupt();
            writeThread.interrupt();
            stopSelf();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getInCount(){
        return incount[0];
    }
    public static long getOutCount(){
        return outcount[0];
    }

    public class LocalBinder extends Binder {
        public ThreadedVpnService getServerInstance() {
            return ThreadedVpnService.this;
        }
    }


}

