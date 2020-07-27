package com.lazerwars2563;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RoomsRecViewAdapter extends RecyclerView.Adapter<RoomsRecViewAdapter.ViewHolder>{

    private ArrayList<Room> rooms = new ArrayList<>();

    private Context context;
    public RoomsRecViewAdapter(Context context) {
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.roomcard,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.room_name.setText(rooms.get(position).getName());
        holder.room_type.setText(rooms.get(position).getType());
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, rooms.get(position).getName() + " Selected", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void setRooms(ArrayList<Room> rooms) {
        this.rooms = rooms;
        //refresh data
        notifyDataSetChanged();
    }

    public class  ViewHolder extends RecyclerView.ViewHolder{
        private TextView room_name;
        private TextView room_type;
        private CardView parent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            room_name = itemView.findViewById(R.id.roomName);
            room_type = itemView.findViewById(R.id.gameType);
            parent = itemView.findViewById(R.id.parent);
        }
    }
}
