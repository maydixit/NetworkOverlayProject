package com.example.may.networkoverlayproject.service;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.github.davidmoten.rx.Bytes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

import io.reactivex.Observer;


import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static android.R.attr.start;
import static android.R.attr.x;


/**
 * Created by May on 5/12/17.
 */

public class OceanVPNService extends VpnService{

    Builder builder = new Builder();
    private ParcelFileDescriptor vpnInterface = null;
    private String REMOTE_ADDR = "104.154.153.166";
    private int REMOTE_PORT = 8888;
    Selector selector;
    SocketChannel tcpChannel = null;

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

            connect(REMOTE_ADDR, REMOTE_PORT).subscribeOn(Schedulers.io()).forEach(socket -> {
                readFromSocket(socket).subscribeOn(Schedulers.io())
                        .forEach(bytes -> {Log.d("reading: ", String.valueOf(bytes.length));
                            try {
                                out.write(bytes) ;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } );
                // replace log.d with out.write(bytes)


                dataReady().subscribeOn(Schedulers.io()).forEach(packet -> {
                    writeToSocket(packet).observeOn(Schedulers.io());
                });

                //in the next line, add an observable for if data to write, which returns us a byte[]
                // call another observable that writes to the network on the socket


                // dataReady().subscribeOn(Schedulers.io())
                //              .forEach(packet -> socket.write(ByteBuffer.wrap(packet)));


            });


        } catch (Exception e) {
            Log.d("OceanVPN", "exception");
            e.printStackTrace();
        }

        return START_STICKY;

    }

    Observable<SocketChannel> connect(String host,int port) {
        // Observable to create a tcp connection
        return Observable.fromCallable(new Callable<SocketChannel>() {
            @Override
            public SocketChannel call() throws Exception {
                tcpChannel = SocketChannel.open();
                tcpChannel.configureBlocking(false);
                tcpChannel.connect(new InetSocketAddress(REMOTE_ADDR, REMOTE_PORT));
                selector = Selector.open();
                tcpChannel.register(selector, SelectionKey.OP_CONNECT);

                while(!tcpChannel.isConnected()) {
                    Log.d("in connect", "returning: " + tcpChannel.toString());
                }
                return tcpChannel;
            }
        });
    }

    Observable<byte[]> readFromSocket(SocketChannel inputchannel) {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                ByteBuffer buffer = ByteBuffer.allocate(48);
                try {
                    inputchannel.read(buffer);
                    subscriber.onNext(buffer.array());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        /*
        */
    }

    Observable<byte[]> dataReady () {
        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        rx.Observable<byte[]> in_chunks = Bytes.from(in);
        // change it to packet with headers here
        return in_chunks;
    }

    Observable<Void> writeToSocket(byte[] buffer){
        return Observable.create(new Observable.OnSubscribe<Void>(){
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    if (tcpChannel.isConnected()) {
                        tcpChannel.write(ByteBuffer.wrap(buffer));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
    }

}

class DataPacket{
    byte[] data;
    SocketChannel socketChannel;
}