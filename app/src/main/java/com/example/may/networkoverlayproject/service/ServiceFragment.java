package com.example.may.networkoverlayproject.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.may.networkoverlayproject.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;


public class ServiceFragment extends Fragment {

    public ServiceFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Observable.interval(1000L, TimeUnit.MILLISECONDS)
                .timeInterval()
                .observeOn(AndroidSchedulers.mainThread())
                .forEach(value -> {
                    updateCounts();
                });


    }

    private void updateCounts() {
        if (ThreadedVpnService.getStatus() ) {

            long in = ThreadedVpnService.getInCount();
            long out = ThreadedVpnService.getOutCount();
            ((TextView) (getActivity().findViewById(R.id.inCount))).setText("In Count: " + String.valueOf(in));
            ((TextView) (getActivity().findViewById(R.id.outCount))).setText("Out Count: " + String.valueOf(out));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_service, container, false);
    }

    /*
        Method to change toggle status and create a new VPN service or stop it.

     */
    public void toggleService(){
        if (getServiceStatus()) stopService();
        else startService();
    }


    private void startService(){

        Intent vpnIntent = VpnService.prepare(getContext());

        if (vpnIntent != null) startActivityForResult(vpnIntent, 0);
        else onActivityResult(0, getActivity().RESULT_OK, null);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (getActivity().RESULT_OK == resultCode) {
            getActivity().startService(new Intent(getContext(), ThreadedVpnService.class));
        }
    }

    private void stopService(){
        //write how to stop
        Log.d("FRAG", "calling alertstop");
        ThreadedVpnService.alertStop();
    }

    public boolean getServiceStatus(){
        return ThreadedVpnService.getStatus();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

}
