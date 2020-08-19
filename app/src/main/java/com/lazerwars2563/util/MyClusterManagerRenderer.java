package com.lazerwars2563.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.google.type.Color;
import com.lazerwars2563.Class.ClusterMarker;
import com.lazerwars2563.R;

public class MyClusterManagerRenderer extends DefaultClusterRenderer<ClusterMarker> {

    private Context thisContext;

    private final IconGenerator iconGenerator;
    private final ImageView imageView;
    private final int markerWidth;
    private final int markerHeight;

    public MyClusterManagerRenderer(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
        super(context, map, clusterManager);
        thisContext = context;

        iconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());
        markerWidth = (int)context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int)context.getResources().getDimension(R.dimen.custom_marker_image);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth,markerHeight));
        int padding = (int)context.getResources().getDimension(R.dimen.custom_marker_padding);
        imageView.setPadding(padding,padding,padding,padding);
        iconGenerator.setContentView(imageView);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull ClusterMarker item, @NonNull MarkerOptions markerOptions) {
        //set teams color
        int color = thisContext.getResources().getIntArray(R.array.TeamsColor)[item.getTeam()];
        ColorDrawable cd = new ColorDrawable(color);
        iconGenerator.setBackground(cd);
        //set picture
        if(item.getIconPicture() != "") {
            Bitmap myBitmap = BitmapFactory.decodeFile(item.getIconPicture());
            imageView.setImageBitmap(myBitmap);
        }
        else //if player has no picture
        {
            imageView.setImageResource(R.drawable.ic_warning_black_24dp);
        }

        Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(item.getTitle());
    }

    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<ClusterMarker> cluster) {
        return false;
    }
}
