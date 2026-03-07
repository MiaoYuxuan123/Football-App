package com.example.football.ui.main.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.football.R;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class HomeStarBannerAdapter extends RecyclerView.Adapter<HomeStarBannerAdapter.StarViewHolder> {

    public static class StarBannerItem {
        @Nullable
        public final Integer photoDrawableRes;
        @Nullable
        public final String photoFilePath;
        @Nullable
        public final String photoAssetPath;
        public final String title;
        public final String subtitle;

        private StarBannerItem(@Nullable Integer photoDrawableRes, @Nullable String photoFilePath,
                               @Nullable String photoAssetPath, String title, String subtitle) {
            this.photoDrawableRes = photoDrawableRes;
            this.photoFilePath = photoFilePath;
            this.photoAssetPath = photoAssetPath;
            this.title = title;
            this.subtitle = subtitle;
        }

        public static StarBannerItem fromDrawable(int photoDrawableRes, String title, String subtitle) {
            return new StarBannerItem(photoDrawableRes, null, null, title, subtitle);
        }

        public static StarBannerItem fromFilePath(@NonNull String photoFilePath, String title, String subtitle) {
            return new StarBannerItem(null, photoFilePath, null, title, subtitle);
        }

        public static StarBannerItem fromAssetPath(@NonNull String photoAssetPath, String title, String subtitle) {
            return new StarBannerItem(null, null, photoAssetPath, title, subtitle);
        }
    }

    private final List<StarBannerItem> items;

    public HomeStarBannerAdapter(@NonNull List<StarBannerItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public StarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_star_banner, parent, false);
        return new StarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StarViewHolder holder, int position) {
        StarBannerItem item = items.get(position);
        if (!TextUtils.isEmpty(item.photoFilePath)) {
            holder.ivHomeStarPhoto.setImageURI(Uri.fromFile(new File(item.photoFilePath)));
        } else if (!TextUtils.isEmpty(item.photoAssetPath)) {
            try (InputStream is = holder.itemView.getContext().getAssets().open(item.photoAssetPath)) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                holder.ivHomeStarPhoto.setImageBitmap(bitmap);
            } catch (Exception ignored) {
                holder.ivHomeStarPhoto.setImageDrawable(null);
            }
        } else if (item.photoDrawableRes != null) {
            holder.ivHomeStarPhoto.setImageResource(item.photoDrawableRes);
        } else {
            holder.ivHomeStarPhoto.setImageDrawable(null);
        }
        holder.tvHomeStarTitle.setText(item.title);
        holder.tvHomeStarSubtitle.setText(item.subtitle);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class StarViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivHomeStarPhoto;
        private final TextView tvHomeStarTitle;
        private final TextView tvHomeStarSubtitle;

        StarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHomeStarPhoto = itemView.findViewById(R.id.iv_home_star_photo);
            tvHomeStarTitle = itemView.findViewById(R.id.tv_home_star_title);
            tvHomeStarSubtitle = itemView.findViewById(R.id.tv_home_star_subtitle);
        }
    }
}
