package com.xuatzsolutions.xuatzmediaplayer2;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by lowxu on 27/9/2015.
 */
public class MySideNavViewAdapter extends RecyclerView.Adapter<MySideNavViewAdapter.ViewHolder> {

    final static String TAG = "MySideNavViewAdapter";

    // Creating a ViewHolder which extends the RecyclerView View Holder
    // ViewHolder are used to to store the inflated views in order to recycle them
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final IMyViewHolderOnClickListener mListener;
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(TextView v, IMyViewHolderOnClickListener listener) {
            super(v);

            mListener = listener;
            mTextView = v;

            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "test1:getLayoutPosition(): " + getLayoutPosition());
                    Log.d(TAG, "test1:getAdapterPosition(): " + getAdapterPosition());

                    Log.d(TAG, "test1:view.getId(): " + view.getId());


//                    int position  =   getAdapterPosition();
//
//                    switch (view.getId()){
//                        case R.id.sharedFile:
//
//
//                            Log.w("", "Selected"+position);
//
//
//                            break;
//                    }
//
//                    mListener.myTest();
                }
            });
        }

        public interface IMyViewHolderOnClickListener {
            void onPotato(View caller);
            void onTomato(String callerImage);
            void myTest(String text);
        }
    }

    private String[] mDataset;

    public MySideNavViewAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    @Override
    public MySideNavViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);

        ViewHolder vh = new ViewHolder((TextView) v);
        return vh;
    }

    //Next we override a method which is called when the item in a row is needed to be displayed, here the int position
    // Tells us item at which position is being constructed to be displayed and the holder id of the holder object tell us
    // which view type is being created 1 for item row
    @Override
    public void onBindViewHolder(MySideNavViewAdapter.ViewHolder holder, int position) {
        holder.mTextView.setText(mDataset[position]);
    }

    // This method returns the number of items present in the list
    @Override
    public int getItemCount() {
        return mDataset.length; // the number of items in the list will be +1 the titles including the header view.
    }

}
