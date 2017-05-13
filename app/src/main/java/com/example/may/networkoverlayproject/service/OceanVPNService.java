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
    Selector selector;
    //SocketChannel tcpChannel = null;
    //Socket tcpSocket = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        super.onStartCommand(intent, flags, startID);

        Log.d("OceanVPN", "oncreate");

        builder.addAddress("192.168.0.1", 24);

        builder.addRoute("0.0.0.0", 0);
        vpnInterface = builder.setSession("OCeansession").establish();

        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());


        try {
            Log.d("OceanVPN", "creating channel");

            connect(REMOTE_ADDR, REMOTE_PORT).subscribeOn(Schedulers.newThread()).forEach(socket -> {
                Log.d("Got socket", socket.toString());
                readFromSocket(socket).subscribeOn(Schedulers.newThread())
                        .forEach(bytes -> {
                            Log.d("reading: ", String.valueOf(bytes.length));
                            try {
                                out.write(bytes) ;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } );
                DataOutputStream outputStream = null;
                try {
                    outputStream = new DataOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DataOutputStream finalOutputStream = outputStream;
                dataReady().subscribeOn(Schedulers.newThread()).forEach(packet -> {

                    if (packet.length == 0)
                        return;
                    //Log.d("dataready", "got data" + packet.toString());
                    if(socket.isConnected()){
                        //Log.d("in write socket", "tcp is connected");
                        try {

                            Log.d("received packet: ", packet.toString());
                            finalOutputStream.writeShort(packet.length);
                            finalOutputStream.writeShort(0);
                            finalOutputStream.write(packet);
                            Log.d("writing to tcp socket", " packet len: " + String.valueOf(packet.length) + " Packet: " + packet.toString() );
                            Log.d("connectino details: ", socket.toString());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });



            });


        } catch (Exception e) {
            Log.d("OceanVPN", "exception");
            e.printStackTrace();
        }

        return START_STICKY;

    }

    Observable<Socket> connect(String host, int port) {
        // Observable to create a tcp connection
        return Observable.fromCallable(new Callable<Socket>() {
            @Override
            public Socket call() throws Exception {

                Socket tcpSocket = new Socket(REMOTE_ADDR, REMOTE_PORT);
                while (!tcpSocket.isConnected()){
                    Log.d("TCP SOCKET", "NOT CONNECTEd " + tcpSocket.toString());
                }
                return tcpSocket;
            }
        });
    }

    Observable<byte[]> readFromSocket(Socket inputchannel) {

        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {

                Log.d("Read from socket", "creating ovservable");

                try {

                    if (inputchannel.isConnected()){
                        Log.d("Read from socket", "setting up data input stream");
                        DataInputStream inputStream = new DataInputStream(inputchannel.getInputStream());
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

        /*
        */
    }

    Observable<byte[]> dataReady () {
        //FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        Log.d("Data ready observable", "returning stream");
        //rx.Observable<byte[]> in_chunks = Bytes.from(in, 512);
        //return in_chunks;

        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {

                Log.d("Read from input", "creating ovservable");

                try {


                    Log.d("Read from input", "setting up data input stream");
                    FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                    ByteBuffer packet = ByteBuffer.allocate(64000);
                    while(true) {
                        int length = in.read(packet.array());
                        if (length > 0) {
                            // Write the outgoing packet to the tunnel.
                            packet.limit(length);
                            byte[] remaining = new byte[packet.remaining()];
                            packet.get(remaining, 0, remaining.length);
                            Log.d("Sending packet: ", remaining.toString());
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

    Observable<Void> writeToSocket(byte[] buffer, Socket socket){
        return Observable.create(new Observable.OnSubscribe<Void>(){
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                Log.d("in write socket", "init");

            }

        });
    }

}

class DataPacket{
    byte[] data;
    SocketChannel socketChannel;
}