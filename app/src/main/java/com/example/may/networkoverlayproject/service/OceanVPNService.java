package com.example.may.networkoverlayproject.service;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.github.davidmoten.rx.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;


import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;


/**
 * Created by May on 5/12/17.
 */

public class OceanVPNService extends VpnService{

    Builder builder = new Builder();
    private ParcelFileDescriptor vpnInterface = null;
    private String REMOTE_ADDR = "104.154.153.166";
    private int REMOTE_PORT = 8888;
    final private int MAX_PACKET_SIZE = 64000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        builder.addAddress("192.168.0.1", 24);
        builder.addRoute("0.0.0.0", 0);
        vpnInterface = builder.setSession("OCeansession").establish();

        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
        try {
            connect(REMOTE_ADDR, REMOTE_PORT)
               .subscribeOn(Schedulers.newThread())
               .forEach(socket -> {
                   Log.d("Got socket", socket.toString());
                   readFromSocket(socket)
                       .subscribeOn(Schedulers.newThread())
                       .forEach(bytes -> {
                           Log.d("reading: ", String.valueOf(bytes.length));
                           try {
                               out.write(bytes);
                               Log.d("Writing to vpn", bytes.toString());
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                       });
                   DataOutputStream outputStream = null;
                   try {
                       outputStream = new DataOutputStream(socket.getOutputStream());
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                   DataOutputStream finalOutputStream = outputStream;
                   packetsReadyUpstream().subscribeOn(Schedulers.newThread()).forEach(packet -> {

                       if (packet.length == 0)
                           return;
                       if(socket.isConnected()){
                           try {

                               finalOutputStream.writeShort(packet.length);
                               finalOutputStream.writeShort(0);
                               finalOutputStream.write(packet);
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                       }
                   });

               });
        }
        catch (Exception e) {
            Log.d("OceanVPN", "exception");
            e.printStackTrace();
        }

        return START_STICKY;

    }

    Observable<Socket> connect(String host, int port) {
        return Observable.fromCallable(new Callable<Socket>() {
            @Override
            public Socket call() throws Exception {
                Socket tcpSocket = new Socket(host, port);
                while (!tcpSocket.isConnected()){
                    Log.d("TCP SOCKET", "NOT CONNECTEd " + tcpSocket.toString());
                }
                return tcpSocket;
            }
        });
    }

    Observable<byte[]> readFromSocket(Socket inputChannel) {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                try {
                    if (inputChannel.isConnected()){
                        DataInputStream inputStream = new DataInputStream(inputChannel.getInputStream());
                        while(true) {
                            short length = inputStream.readShort();
                            inputStream.readShort();
                            byte[] inPacket = new byte[length];
                            inputStream.readFully(inPacket);
                            subscriber.onNext(inPacket);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    Observable<byte[]> packetsReadyUpstream () {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                try {
                    FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                    ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE);
                    while(true) {
                        int length = in.read(packet.array());
                        if (length > 0) {
                            packet.limit(length);
                            byte[] remaining = new byte[packet.remaining()];
                            packet.get(remaining, 0, remaining.length);
                            subscriber.onNext(remaining);
                            packet.clear();
                        }
                    }
                    } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }
}
