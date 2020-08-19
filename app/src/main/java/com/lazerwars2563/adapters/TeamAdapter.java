package com.lazerwars2563.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lazerwars2563.R;
import com.lazerwars2563.Class.TeamItem;

import java.util.ArrayList;

public class TeamAdapter extends ArrayAdapter<TeamItem> {
    public TeamAdapter(Context context, ArrayList<TeamItem> teamList)
    {
        super(context, 0, teamList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position,convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position,convertView, parent);
    }

    private View initView(int position,  View convertView, ViewGroup parent)
    {
        if(convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.team_spinner_row,parent,false
            );
        }
        TextView textViewName = convertView.findViewById(R.id.team);

        TeamItem currentItem = getItem(position);
        if(currentItem != null) {
            textViewName.setText(currentItem.getName());
            textViewName.setTextColor(currentItem.getColor());
        }
        return  convertView;
    }
}
