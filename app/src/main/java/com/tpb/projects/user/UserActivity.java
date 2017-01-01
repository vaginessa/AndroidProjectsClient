/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.auth.OAuthHandler;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.login.LoginActivity;
import com.tpb.projects.repo.RepoActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.ShortcutDialog;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 14/12/16.
 * Possible markdown textviews
 * <p>
 * https://github.com/fiskurgit/MarkdownView
 * https://github.com/mukeshsolanki/MarkdownView-Android
 * https://github.com/mittsuu/MarkedView-for-Android
 * https://github.com/falnatsheh/MarkdownView best demo
 */

public class UserActivity extends AppCompatActivity implements UserReposAdapter.RepositoriesManager {
    private static final String TAG = UserActivity.class.getSimpleName();
    private static final String URL = "https://github.com/tpb1908/AndroidProjectsClient/blob/master/app/src/main/java/com/tpb/projects/feed/UserActivity.java";

    private FirebaseAnalytics mAnalytics;
    private ShareActionProvider mShareActionProvider;

    private OAuthHandler mApp;
    @BindView(R.id.repos_refresher) SwipeRefreshLayout mRefresher;
    @BindView(R.id.repos_recycler) AnimatingRecycler mRecycler;
    @BindView(R.id.repos_toolbar) Toolbar mToolbar;
    @BindView(R.id.repos_appbar) AppBarLayout mAppbar;

    @BindView(R.id.user_image) ANImageView mUserImage;
    @BindView(R.id.user_name) TextView mUserName;

    private UserReposAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_user);

        ButterKnife.bind(this);
        AndroidNetworking.initialize(this);

        mAnalytics = FirebaseAnalytics.getInstance(this);
        mAnalytics.setAnalyticsCollectionEnabled(prefs.areAnalyticsEnabled());
        mAnalytics.logEvent(Analytics.TAG_OPEN_REPOS_ACTIVITY, null);

        mApp = new OAuthHandler(this, BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, BuildConfig.GITHUB_REDIRECT_URL);
        if(!mApp.hasAccessToken()) {
            startActivity(new Intent(UserActivity.this, LoginActivity.class));
            finish();
        } else {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            mRecycler.setLayoutManager(new LinearLayoutManager(this));
            mAdapter = new UserReposAdapter(this, this, mRecycler, mRefresher);
            mRecycler.setAdapter(mAdapter);
            final String user;
            if(getIntent() != null && getIntent().hasExtra(getString(R.string.intent_username))) {
                user = getIntent().getStringExtra(getString(R.string.intent_username));
                mUserName.setText(user);
                ((TextView) findViewById(R.id.title_user)).setText(R.string.title_activity_user);
                new Loader(this).loadUser(new Loader.UserLoader() {
                    @Override
                    public void userLoaded(User user) {
                        mUserName.setText(user.getLogin());
                        mUserImage.setImageUrl(user.getAvatarUrl());
                    }

                    @Override
                    public void userLoadError() {

                    }
                }, user);

            } else {
                user = mApp.getUserName();
                ((TextView) findViewById(R.id.title_user)).setText(getTitle());
                if(isTaskRoot()) {
                    findViewById(R.id.back_button).setVisibility(View.GONE);
                }
                new Loader(this).loadAuthenticateUser(new Loader.AuthenticatedUserLoader() {
                    @Override
                    public void userLoaded(User user) {
                        mUserName.setText(user.getLogin());
                        mUserImage.setImageUrl(user.getAvatarUrl());
                        GitHubSession.getSession(UserActivity.this).updateUserInfo(user.getLogin());
                    }

                    @Override
                    public void authenticatedUserLoadError() {

                    }
                });
            }
            mUserName.setText(user);

            if(getIntent().hasExtra(getString(R.string.intent_drawable))) {
                Log.i(TAG, "onCreate: Getting bitmap");
                final Bitmap bm = getIntent().getParcelableExtra(getString(R.string.intent_drawable));
                mUserImage.setBackgroundDrawable(new BitmapDrawable(getResources(), bm));

            }

            mAdapter.loadReposForUser(user);

            mApp.validateKey(isValid -> {
                if(!isValid) {
                    Toast.makeText(UserActivity.this, R.string.error_key_invalid, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(UserActivity.this, LoginActivity.class));
                }
            });
        }
    }

    @Override
    public void openRepo(Repository repo, View view) {
        final Intent i = new Intent(UserActivity.this, RepoActivity.class);
        i.putExtra(getString(R.string.intent_repo), repo);
        mRecycler.disableAnimation();
        startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                view,
                getString(R.string.transition_name)
                ).toBundle()
        );
        overridePendingTransition(R.anim.slide_up, R.anim.none);
        mAnalytics.logEvent(Analytics.TAG_OPEN_REPO, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity, menu);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.menu_share));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(UserActivity.this, SettingsActivity.class));
                break;
            case R.id.menu_source:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
                break;
            case R.id.menu_share:
                mShareActionProvider.setShareIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + mApp.getUserName())));
                break;
            case R.id.menu_save_to_homescreen:
                final ShortcutDialog dialog = new ShortcutDialog();
                final Bundle args = new Bundle();
                args.putInt(getString(R.string.intent_title_res), R.string.title_save_user_shortcut);
                args.putBoolean(getString(R.string.intent_drawable), mUserImage.getDrawable() != null);
                args.putString(getString(R.string.intent_name), mUserName.getText().toString());

                dialog.setArguments(args);
                dialog.setListener((name, iconFlag) -> {
                    final Intent i = new Intent(getApplicationContext(), UserActivity.class);
                    i.putExtra(getString(R.string.intent_username), mUserName.getText().toString());

                    final Intent add = new Intent();
                    add.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
                    add.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                    add.putExtra("duplicate", false);
                    if(iconFlag) {
                        add.putExtra(Intent.EXTRA_SHORTCUT_ICON, ((BitmapDrawable) mUserImage.getDrawable()).getBitmap());
                    } else {
                        add.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    }
                    add.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(add);
                });
                dialog.show(getSupportFragmentManager(), TAG);
                break;
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnalytics.setAnalyticsCollectionEnabled(SettingsActivity.Preferences.getPreferences(this).areAnalyticsEnabled());
    }

    public void onToolbarBackPressed(View view) {
        onBackPressed();
    }

}
