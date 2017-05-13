package com.example.may.networkoverlayproject.service;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.may.networkoverlayproject.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ServiceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ServiceFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private static  boolean TOGGLE_STATUS = false;
    private OceanVPNService vpnService;

    public ServiceFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO : Create the VPN service instance here
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

    private void startService(){
        TOGGLE_STATUS = true;
        Intent vpnIntent = VpnService.prepare(getContext());
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, 0);
        else {
            Log.d("Activity result", "Starting service manually");
            onActivityResult(0, 10, null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("Activity result", "Starting service");
        super.onActivityResult(requestCode, resultCode, data);
        getActivity().startService(new Intent(getContext(), OceanVPNService.class));
    }

    private void stopService(){
        TOGGLE_STATUS = false;

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
