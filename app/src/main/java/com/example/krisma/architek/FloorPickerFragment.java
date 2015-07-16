package com.example.krisma.architek;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.plus.PlusOneButton;

/**
 * A fragment with a Google +1 button.
 * Activities that contain this fragment must implement the
 * {@link FloorPickerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FloorPickerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FloorPickerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_MAX_FLOOR = "ARG_MAX_FLOOR";
    private static final String ARG_DEFAULT_FLOOR = "ARG_DEFAULT_FLOOR";

    // The URL to +1.  Must be a valid URL.
    private final String PLUS_ONE_URL = "http://developer.android.com";

    // The request code must be 0 or greater.
    private static final int PLUS_ONE_REQUEST_CODE = 0;

    private FloatingActionButton mPlusOneButton;
    private FloatingActionButton mMinusOneButton;

    private OnFragmentInteractionListener mListener;
    private TextView floorView;
    private int currentFloor = 0;
    private int maxFloors;
    private String defaultFloor;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FloorPickerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FloorPickerFragment newInstance(int maxFloors, int defaultFloor) {
        FloorPickerFragment fragment = new FloorPickerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MAX_FLOOR, maxFloors);
        args.putInt(ARG_DEFAULT_FLOOR, defaultFloor);
        fragment.setArguments(args);
        return fragment;
    }

    public FloorPickerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            maxFloors = getArguments().getInt(ARG_MAX_FLOOR);
            defaultFloor = getArguments().getString(ARG_DEFAULT_FLOOR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_floor_picker, container, false);

        //Find the buttons
        mPlusOneButton = (FloatingActionButton) view.findViewById(R.id.upButton);
        mMinusOneButton = (FloatingActionButton) view.findViewById(R.id.downButton);

        // Find FloorView
        floorView = (TextView) view.findViewById(R.id.floorView);
        floorView.setShadowLayer(16, 4, 4, Color.BLACK);


        mPlusOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFloor++;
                floorView.setText("Floor:\n\n" + currentFloor);

                if (mListener != null) {
                    mListener.onFragmentInteraction(currentFloor);
                }
            }
        });

        mMinusOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFloor--;
                floorView.setText("Floor:\n\n" + currentFloor);

                if (mListener != null) {
                    mListener.onFragmentInteraction(currentFloor);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(int currentFloor);
    }

}
