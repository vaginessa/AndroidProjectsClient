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

package com.tpb.projects.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.data.auth.OAuthHandler;
import com.tpb.projects.data.models.User;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private FirebaseAnalytics mAnalytics;
    private OAuthHandler mOAuthHandler;
    private boolean mLoginShown = false;

    @BindView(R.id.login_webview) WebView mWebView;
    @BindView(R.id.login_form) CardView mLogin;
    @BindView(R.id.progress_spinner) ProgressBar mSpinner;
    @BindView(R.id.user_details) View mDetails;
    @BindView(R.id.user_image) ANImageView mImage;
    @BindView(R.id.user_name) TextView mName;
    @BindView(R.id.user_id) TextView mId;
    @BindView(R.id.user_stats) TextView mStats;

    private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT);

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        AndroidNetworking.initialize(this);

        mAnalytics = FirebaseAnalytics.getInstance(this);

        mOAuthHandler = new OAuthHandler(this, BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET, BuildConfig.GITHUB_REDIRECT_URL);
        mOAuthHandler.setListener(new OAuthHandler.OAuthAuthenticationListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                Log.i(TAG, "onSuccess: User " + mOAuthHandler.getUserName());
                mWebView.setVisibility(View.GONE);
                mSpinner.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFail(String error) {
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.TAG_LOGIN, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
            }

            @Override
            public void userLoaded(User user) {
                mSpinner.setVisibility(View.GONE);
                mDetails.setVisibility(View.VISIBLE);
                mImage.setImageUrl(user.getAvatarUrl());
                mName.setText(user.getName());
                mId.setText(user.getLogin());
                String details = "";
                if(!user.getBio().equals(Constants.JSON_NULL)) details += user.getBio();
                mStats.setText(String.format(getString(R.string.text_user_info), details, user.getLocation(), user.getRepos(), user.getFollowers()));

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.TAG_LOGIN, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

                new Handler().postDelayed(() -> {
                    startActivity(new Intent(LoginActivity.this, UserActivity.class));
                    finish();
                }, 1500);
            }
        });
        CookieSyncManager.createInstance(this);
        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new OAuthWebViewClient(mOAuthHandler.getListener()));
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mOAuthHandler.getAuthUrl());
        mWebView.setLayoutParams(FILL);
        UI.expand(mLogin);
    }

    private void ensureWebViewVisible() {
        if(!mLoginShown) {
            new Handler().postDelayed(() -> {
                mWebView.setVisibility(View.VISIBLE);
                mSpinner.setVisibility(View.GONE);
                mLoginShown = true;
            }, 150);

        }

    }

    public interface OAuthLoginListener {
        void onComplete(String accessToken);

        void onError(String error);
    }

    private class OAuthWebViewClient extends WebViewClient {
        private final OAuthLoginListener mListener;

        OAuthWebViewClient(OAuthLoginListener listener) {
            mListener = listener;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Redirecting URL " + url);

            if(url.startsWith(OAuthHandler.mCallbackUrl)) {
                String urls[] = url.split("=");
                mListener.onComplete(urls[1]);
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            Log.d(TAG, "Page error: " + description);

            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(description);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if(request.getUrl().toString().startsWith("https://github.com/login/oauth/authorize?") ||
                    request.getUrl().toString().startsWith("https://github.com/session") ||
                    request.getUrl().toString().startsWith("https://github.com/sessions/two-factor") ||
                    request.getUrl().toString().startsWith("https://github.com/password_reset")) {
                return super.shouldOverrideUrlLoading(view, request);
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "Loading URL: " + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            ensureWebViewVisible();
            super.onPageFinished(view, url);
        }

    }

}

