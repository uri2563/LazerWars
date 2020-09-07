package com.lazerwars2563.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lazerwars2563.Activitys.ChooseRoomActivity;
import com.lazerwars2563.Activitys.WaitingRoomActivity;
import com.lazerwars2563.Class.Room;
import com.lazerwars2563.R;
import com.lazerwars2563.util.UserClient;

import java.util.ArrayList;
import java.util.List;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.RoomHolders> implements Filterable {
    private static String TAG = "RoomsAdapter";

    private List<Room> mTubeListFiltered;
    private List<Room> mTubeList;
    private Context thisContext;

        public RoomsAdapter(List<Room> tubeList, Context context ){
            Log.d(TAG,"create");
            mTubeList = tubeList;
            mTubeListFiltered = tubeList;
            this.thisContext = context;
        }

        @NonNull
        @Override
        public RoomHolders onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View tubeView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.roomcard, viewGroup, false);
            return new RoomHolders(tubeView);
        }

        @Override
        public void onBindViewHolder(@NonNull RoomHolders holder, int i) {
            final Room item = mTubeListFiltered.get(i);
            holder.textViewRoomName.setText(item.getName());
            holder.textViewType.setText(item.getGame());

            holder.buttonEnter.setEnabled(item.isRecruiting());

            holder.buttonEnter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //change activity
                    if(UserClient.getInstance().getGameId().equals("None"))
                    {
                        Toast.makeText(thisContext,"Poor Usb connation, please wait and try agine if it isnt working please replug cable",Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Intent intent = new Intent(thisContext, WaitingRoomActivity.class);
                        intent.putExtra("name",item.getName());
                        intent.putExtra("game",item.getGame());
                        intent.putExtra("admin", false);
                        thisContext.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mTubeListFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    String pattern = constraint.toString().toLowerCase();
                    if(pattern.isEmpty()){
                        mTubeListFiltered = mTubeList;
                    } else {
                        List<Room> filteredList = new ArrayList<>();
                        for(Room tube: mTubeList){
                            if(tube.getName().toLowerCase().contains(pattern) || tube.getName().toLowerCase().contains(pattern)) {
                                filteredList.add(tube);
                            }
                        }
                        mTubeListFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = mTubeListFiltered;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mTubeListFiltered = (ArrayList<Room>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        public static class RoomHolders extends RecyclerView.ViewHolder  {
        TextView textViewRoomName;
        TextView textViewType;
        Button buttonEnter;

            public RoomHolders(@NonNull View itemView) {
            super(itemView);
            textViewRoomName = itemView.findViewById(R.id.roomName);
            textViewType = itemView.findViewById(R.id.gameType);
            buttonEnter = itemView.findViewById(R.id.enterButton);
            }
        }
}