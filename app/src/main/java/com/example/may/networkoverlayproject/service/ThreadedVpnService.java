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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ThreadedVpnService extends VpnService {
    public ThreadedVpnService() {
    }


    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    //a. Configure a builder for the interface.
    Builder builder = new Builder();
    private ParcelFileDescriptor vpnInterface = null;
    private String REMOTE_ADDR = "104.154.153.166";
    private int REMOTE_PORT = 8888;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Start a new session by creating a new thread.
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mInterface = builder.setSession("MyVPNService")
                            .addAddress("192.168.0.1", 24)
                            .addDnsServer("8.8.8.8")
                            .addRoute("0.0.0.0", 0).establish();
                    FileInputStream in = new FileInputStream( mInterface.getFileDescriptor());
                    FileOutputStream out = new FileOutputStream( mInterface.getFileDescriptor());

                    Socket tunnel = new Socket();
                    tunnel.connect(new InetSocketAddress(REMOTE_ADDR, REMOTE_PORT));
                    protect(tunnel);

                    Log.d("Connected", tunnel.toString());

                    DataInputStream dataInputStream = new DataInputStream(tunnel.getInputStream());
                    DataOutputStream dataOutputStream = new DataOutputStream(tunnel.getOutputStream());

                    Thread readThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(!Thread.interrupted()){
                                Log.d("OUTGOING", "input available");
                                ByteBuffer packet = ByteBuffer.allocate(64000);
                                int length = 0;
                                try {
                                    length = in.read(packet.array());

                                if (length > 0){
                                    Log.d("OUTGOING", "input length > 0");
                                    packet.limit(length);
                                    byte[] remaining = new byte[packet.remaining()];
                                    packet.get(remaining, 0, remaining.length);
                                    Log.d("OUTGOING", "writing to dataoutputstream");
                                    dataOutputStream.writeShort(length);
                                    dataOutputStream.writeShort(0);
                                    dataOutputStream.write(remaining);
                                    Log.d("OUTGOING", "done writing to dataoutputstream");


                                }
                                    Thread.sleep(100);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    readThread.start();

                    while (!Thread.interrupted()) {
                        
                        Thread.sleep(100);
                            Log.d("INCOMING", "data available");
                            int length = dataInputStream.readShort();
                            dataInputStream.readShort();
                            Log.d("INCOMING", "read length etc");
                            byte[] data = new byte[length];
                            Log.d("INCOMING", "reading data");
                            dataInputStream.read(data);
                            Log.d("INCOMING", "writing to vpn out");
                            out.write(data);
                            Log.d("INCOMING", "done writing");


                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    // Catch any exception
                    e.printStackTrace();
                } finally {
                    try {
                        if (mInterface != null) {
                            mInterface.close();
                            mInterface = null;
                        }
                    } catch (Exception e) {

                    }
                }
            }

        }, "MyVpnRunnable");

        //start the service
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (mThread != null) {
            mThread.interrupt();
        }
        super.onDestroy();
    }

}
