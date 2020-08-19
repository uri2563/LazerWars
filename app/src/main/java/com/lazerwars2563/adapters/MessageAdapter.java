package com.lazerwars2563.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lazerwars2563.Class.Message;
import com.lazerwars2563.Class.PlayerViewer;
import com.lazerwars2563.R;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolders> {
    private static String TAG = "MessageAdapter";

    private List<Message> mTubeListFiltered;
    private List<Message> mTubeList;
    private Context thisContext;
    private PlayerViewer myData;

    public MessageAdapter(List<Message> tubeList, Context context, PlayerViewer myData ){
        mTubeList = tubeList;
        mTubeListFiltered = tubeList;
        this.thisContext = context;
        this.myData = myData;
    }

    @NonNull
    @Override
    public MessageHolders onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Log.d(TAG,"onCreateViewHolder");
        View tubeView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.chat_view, viewGroup, false);
        return new MessageHolders(tubeView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolders holder, int i) {
        Log.d(TAG,"onBindViewHolder");
        final Message item = mTubeListFiltered.get(i);
        holder.textViewMessage.setText(item.getContent());
    }

    @Override
    public int getItemCount() {
        return mTubeListFiltered.size();
    }


    public static class MessageHolders extends RecyclerView.ViewHolder  {
        TextView textViewMessage;

        public MessageHolders(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_message);
        }
    }
}