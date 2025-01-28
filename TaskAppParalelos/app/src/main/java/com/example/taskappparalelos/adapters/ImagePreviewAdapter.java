package com.example.taskappparalelos.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.taskappparalelos.R;

import java.util.List;

public class ImagePreviewAdapter extends BaseAdapter {
    private final Context context;
    private final List<Uri> imageUris;

    public ImagePreviewAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    @Override
    public int getCount() {
        return imageUris.size();
    }

    @Override
    public Object getItem(int position) {
        return imageUris.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.imagePreview);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Uri imageUri = imageUris.get(position);

        // Usa Glide para cargar la imagen
        Glide.with(context).load(imageUri).into(holder.imageView);

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
    }
}
