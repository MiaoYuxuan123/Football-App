package com.example.football.ui.main.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.football.R;
import com.example.football.ui.main.model.BadgeDisplayItem;

import java.util.ArrayList;
import java.util.List;

public class BadgeHallAdapter extends RecyclerView.Adapter<BadgeHallAdapter.BadgeViewHolder> {

    private final List<BadgeDisplayItem> items = new ArrayList<>();

    public void submitList(List<BadgeDisplayItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_hall, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final View card;
        private final TextView tvBadgeMark;
        private final TextView tvBadgeTitle;
        private final TextView tvBadgeSubtitle;
        private final TextView tvBadgeStatus;
        private final ImageView ivBadgeIcon;

        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.layoutBadgeCard);
            tvBadgeMark = itemView.findViewById(R.id.tvBadgeMark);
            tvBadgeTitle = itemView.findViewById(R.id.tvBadgeTitle);
            tvBadgeSubtitle = itemView.findViewById(R.id.tvBadgeSubtitle);
            tvBadgeStatus = itemView.findViewById(R.id.tvBadgeStatus);
            ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
        }

        void bind(BadgeDisplayItem item) {
            tvBadgeTitle.setText(item.title);
            tvBadgeSubtitle.setText(item.subtitle);
            tvBadgeMark.setText(item.unlocked ? "#" : "-");
            tvBadgeStatus.setText(item.unlocked
                    ? itemView.getContext().getString(R.string.badge_hall_status_unlocked)
                    : itemView.getContext().getString(R.string.badge_hall_status_locked));

            if (item.unlocked) {
                card.setBackgroundResource(R.drawable.bg_badge_card_unlocked);
                tvBadgeMark.setTextColor(0xFFB2FF1A);
                tvBadgeTitle.setTextColor(0xFFFFFFFF);
                tvBadgeSubtitle.setTextColor(0xFFDDE7F4);
                tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_status_unlocked);
                tvBadgeStatus.setTextColor(0xFF10140B);
                ivBadgeIcon.setAlpha(1f);
                ivBadgeIcon.setColorFilter(null);
                ivBadgeIcon.setScaleX(1.15f);
                ivBadgeIcon.setScaleY(1.15f);
                ivBadgeIcon.setRotation(0f);
                itemView.setAlpha(1f);
            } else {
                card.setBackgroundResource(R.drawable.bg_badge_card_locked);
                tvBadgeMark.setTextColor(0xFF7D8999);
                tvBadgeTitle.setTextColor(0xFFBBC3CD);
                tvBadgeSubtitle.setTextColor(0xFF8C95A3);
                tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_status_locked);
                tvBadgeStatus.setTextColor(0xFFC2CAD4);
                ivBadgeIcon.setAlpha(0.5f);
                ivBadgeIcon.setColorFilter(0xFFB0B0B0);
                ivBadgeIcon.setScaleX(1f);
                ivBadgeIcon.setScaleY(1f);
                ivBadgeIcon.setRotation(-15f);
                itemView.setAlpha(0.92f);
            }
        }
    }
}
