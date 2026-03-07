package com.example.football.ui.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.ui.main.fragments.HomeFragment;
import com.example.football.ui.main.fragments.MilestoneFragment;
import com.example.football.ui.main.fragments.MineFragment;
import com.example.football.ui.main.fragments.TrainFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_TRAIN = "tab_train";
    private static final String TAG_MILESTONE = "tab_milestone";
    private static final String TAG_MINE = "tab_mine";
    private static final String KEY_CURRENT_TAG = "main_current_tag";

    private Fragment homeFragment;
    private Fragment trainFragment;
    private Fragment milestoneFragment;
    private Fragment mineFragment;
    private String currentTag = TAG_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFragments(savedInstanceState);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchTo(TAG_HOME);
                return true;
            } else if (itemId == R.id.nav_train) {
                switchTo(TAG_TRAIN);
                return true;
            } else if (itemId == R.id.nav_milestone) {
                switchTo(TAG_MILESTONE);
                return true;
            } else if (itemId == R.id.nav_mine) {
                switchTo(TAG_MINE);
                return true;
            }
            return false;
        });

        if (TAG_TRAIN.equals(currentTag)) {
            bottomNav.setSelectedItemId(R.id.nav_train);
        } else if (TAG_MILESTONE.equals(currentTag)) {
            bottomNav.setSelectedItemId(R.id.nav_milestone);
        } else if (TAG_MINE.equals(currentTag)) {
            bottomNav.setSelectedItemId(R.id.nav_mine);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            trainFragment = new TrainFragment();
            milestoneFragment = new MilestoneFragment();
            mineFragment = new MineFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, homeFragment, TAG_HOME)
                    .add(R.id.fragment_container, trainFragment, TAG_TRAIN)
                    .hide(trainFragment)
                    .add(R.id.fragment_container, milestoneFragment, TAG_MILESTONE)
                    .hide(milestoneFragment)
                    .add(R.id.fragment_container, mineFragment, TAG_MINE)
                    .hide(mineFragment)
                    .commit();
            currentTag = TAG_HOME;
            return;
        }

        homeFragment = requireFragment(TAG_HOME, new HomeFragment());
        trainFragment = requireFragment(TAG_TRAIN, new TrainFragment());
        milestoneFragment = requireFragment(TAG_MILESTONE, new MilestoneFragment());
        mineFragment = requireFragment(TAG_MINE, new MineFragment());
        currentTag = savedInstanceState.getString(KEY_CURRENT_TAG, TAG_HOME);
    }

    @NonNull
    private Fragment requireFragment(@NonNull String tag, @NonNull Fragment fallback) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        return fragment != null ? fragment : fallback;
    }

    private void switchTo(@NonNull String targetTag) {
        if (targetTag.equals(currentTag)) {
            return;
        }
        Fragment current = getFragmentByTag(currentTag);
        Fragment target = getFragmentByTag(targetTag);
        if (target == null) {
            return;
        }

        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (current != null && current.isAdded()) {
            transaction.hide(current);
        }
        if (target.isAdded()) {
            transaction.show(target);
        } else {
            transaction.add(R.id.fragment_container, target, targetTag);
        }
        transaction.commit();
        currentTag = targetTag;
    }

    /**
     * Compatibility entry kept for existing callers.
     * Prefer bottom-tab switching for main tabs to keep fragment instances alive.
     */
    public void replaceFragment(@NonNull Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            switchTo(TAG_HOME);
            return;
        }
        if (fragment instanceof TrainFragment) {
            switchTo(TAG_TRAIN);
            return;
        }
        if (fragment instanceof MilestoneFragment) {
            switchTo(TAG_MILESTONE);
            return;
        }
        if (fragment instanceof MineFragment) {
            switchTo(TAG_MINE);
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private Fragment getFragmentByTag(@NonNull String tag) {
        if (TAG_HOME.equals(tag)) {
            return homeFragment;
        }
        if (TAG_TRAIN.equals(tag)) {
            return trainFragment;
        }
        if (TAG_MILESTONE.equals(tag)) {
            return milestoneFragment;
        }
        if (TAG_MINE.equals(tag)) {
            return mineFragment;
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_TAG, currentTag);
    }
}