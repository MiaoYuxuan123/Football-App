package com.example.football.ui.main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.ui.main.model.BadgeDisplayItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BadgeHallFragment extends Fragment {

    private AppRepository repository;
    private BadgeHallAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_badge_hall, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.get(requireContext());

        TextView btnBack = view.findViewById(R.id.btnBack);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerBadges);

        adapter = new BadgeHallAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        bindBadges();
    }

    private void bindBadges() {
        String account = repository.getCurrentAccount();
        MilestoneData data = repository.getMilestone(account);
        MilestoneDbHelper.TrainingSummary summary = repository.getTrainingSummary(account);

        Set<String> unlockedFromSaved = parseUnlockedBadgeNames(data.badgesJson);
        int avgScore = summary.avgScore == 0 ? 70 : summary.avgScore;

        List<BadgeDisplayItem> list = new ArrayList<>();
        list.add(build("shoot_starter", "初级射手", "单项射门达到 10 次", data.shootCount >= 10 || unlockedFromSaved.contains("初级射手")));
        list.add(build("streak_3", "连训三天", "累计训练达到 3 次", data.trainCount >= 3 || unlockedFromSaved.contains("连训3天")));
        list.add(build("shoot_100", "百次射门", "射门累计 100 次", data.shootCount >= 100 || unlockedFromSaved.contains("百次射门")));
        list.add(build("pass_core", "传球指挥官", "传球累计 50 次", data.passCount >= 50));
        list.add(build("dribble_core", "运球魔术师", "运球累计 50 次", data.dribbleCount >= 50));
        list.add(build("train_30", "训练狂热者", "总训练次数达到 30", data.trainCount >= 30));
        list.add(build("stable_75", "稳定输出", "平均评分达到 75+", avgScore >= 75));
        list.add(build("elite_85", "A 级精度", "平均评分达到 85+", avgScore >= 85));
        list.add(build("all_round", "全能战士", "三项训练都达到 30 次", data.shootCount >= 30 && data.passCount >= 30 && data.dribbleCount >= 30));
        list.add(build("legend_5", "里程碑征服者", "等级达到 5 级", data.level >= 5));

        adapter.submitList(list);
    }

    private BadgeDisplayItem build(String id, String title, String subtitle, boolean unlocked) {
        return new BadgeDisplayItem(id, title, subtitle, unlocked);
    }

    private Set<String> parseUnlockedBadgeNames(String json) {
        Set<String> result = new HashSet<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        try {
            Type type = new TypeToken<List<SavedBadge>>() { }.getType();
            List<SavedBadge> list = new Gson().fromJson(json, type);
            if (list == null) {
                return result;
            }
            for (SavedBadge badge : list) {
                if (badge != null && badge.unlocked && badge.name != null) {
                    result.add(badge.name);
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed old data and keep derived unlock rules.
        }
        return result;
    }

    private static class SavedBadge {
        String name;
        boolean unlocked;
    }
}
