package com.example.may.networkoverlayproject.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.may.networkoverlayproject.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ServiceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ServiceFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static  boolean TOGGLE_STATUS = false;
    private ThreadedVpnService vpnService;
    private ThreadedVpnService mService;

    public ServiceFragment() {
        // Required empty public constructor
    }


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
        long in = ThreadedVpnService.getInCount();
        long out = ThreadedVpnService.getOutCount();
        if(TOGGLE_STATUS) {
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
        Log.d("TAG", "in fragment");
        if (getServiceStatus()){
            stopService();
        }
        else{
            startService();
        }
    }

    private boolean bound;
    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            bound = false;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            ThreadedVpnService.LocalBinder mLocalBinder = (ThreadedVpnService.LocalBinder)service;
            mService = mLocalBinder.getServerInstance();
            bound = true;
        }
    };
    private void startService(){
        TOGGLE_STATUS = true;
        Intent vpnIntent = VpnService.prepare(getContext());

        if (vpnIntent != null)
            startActivityForResult(vpnIntent, 0);
        else {
            Log.d("Activity result", "Starting service manually");
            onActivityResult(0, getActivity().RESULT_OK, null);
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("Activity result", "Starting service");
        super.onActivityResult(requestCode, resultCode, data);

        if (getActivity().RESULT_OK == resultCode) {
            Intent vpnIntent = new Intent(getContext(), ThreadedVpnService.class);
            getActivity().bindService(vpnIntent, mConnection, getActivity().BIND_AUTO_CREATE);
            getActivity().startService(vpnIntent);
        }
    }

    private void stopService(){
        TOGGLE_STATUS = false;
        mService.stopVpn();

    }

    public boolean getServiceStatus(){

        return TOGGLE_STATUS;


    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
/*        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStop(){
        super.onStop();
        if(bound) {
            getActivity().unbindService(mConnection);
            bound = false;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
