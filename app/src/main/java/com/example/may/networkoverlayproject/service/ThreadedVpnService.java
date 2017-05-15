package com.example.may.networkoverlayproject.service;

import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
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
import java.nio.channels.SocketChannel;

public class ThreadedVpnService extends VpnService {
    private static boolean RUNNING = false;
    private static boolean stop = false;

    public ThreadedVpnService() {
    }

    int retryCount = 0;
    final int MAX_RETRY_COUNT = 3;
    private Thread readThread, writeThread;
    Builder builder = new Builder();
    private ParcelFileDescriptor vpnInterface = null;
    private String REMOTE_ADDR = "104.154.153.166";
    private int REMOTE_PORT = 8888;
    static final long[] incount = {0};
    static final long[] outcount = {0};

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setStatus(true);
        // Start a new session by creating a new thread.
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Log.d("THREAD", String.valueOf(retryCount));
                Log.d("THREAD", String.valueOf(stop));
                while (retryCount < MAX_RETRY_COUNT && !stop) {

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
                                while (!Thread.interrupted() && !stop) {
                                    ByteBuffer packet = ByteBuffer.allocate(64000);
                                    int length = 0;
                                    try {
                                        length = in.read(packet.array());

                                        if (length > 0) {
                                            packet.limit(length);
                                            byte[] remaining = new byte[packet.remaining()];
                                            packet.get(remaining, 0, remaining.length);
                                            dataOutputStream.writeShort(length);
                                            dataOutputStream.writeShort(0);
                                            dataOutputStream.write(remaining);
                                            outcount[0]++;
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

                        while (!Thread.interrupted() && !stop) {
                            int length = dataInputStream.readShort();
                            dataInputStream.readShort();

                            byte[] data = new byte[length];
                            dataInputStream.read(data);
                            out.write(data);

                            incount[0]++;
                            Thread.sleep(100);
                        }

                    } catch (Exception e) {
                        // Catch any exception
                        e.printStackTrace();
                    } finally {
                        try {
                            if (vpnInterface != null) {
                                Log.d("THREAD", "interrupted");
                                vpnInterface.close();
                                vpnInterface = null;
                            }
                        } catch (Exception e) {

                        }
                        retryCount++;
                    }
                }
            stopSelf();
            }

        }, "MyVpnRunnable");

        //start the service
        readThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        setStatus(false);
        if (readThread != null) {
            readThread.interrupt();
        }
        if (writeThread != null){
            writeThread.interrupt();
        }
        if(vpnInterface != null){
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }



    public static long getInCount(){
        return incount[0];
    }
    public static long getOutCount(){
        return outcount[0];
    }
    public static boolean getStatus(){
        return RUNNING;
    }

    private void setStatus(boolean status) {
        this.RUNNING = status;
        this.stop = !status;
    }
    public static void alertStop(){
        stop = true;
    }
}