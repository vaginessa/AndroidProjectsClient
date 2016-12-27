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

package com.tpb.projects.util;

import com.tpb.projects.BuildConfig;

/**
 * Created by theo on 14/12/16.
 */

public class Constants {
    public static final String CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID;
    public static final String CLIENT_SECRET = BuildConfig.GITHUB_CLIENT_SECRET;
    public static final String GIT_API_ROOT_URL = "https://api.github.com/";
    public static final String AUTH_SERVER_URL = "https://github.com/login/oauth/authorize";
    public static final String TOKEN_SERVER_URL = "https://github.com/login/oauth/access_token";
    public static final String REDIRECT_URL = "https://github.com/tpb1908/AndroidProjectsClient";
    public static final String CREDENTIALS_STORE_PREF_FILE = "oauth";

    public static final String JSON_KEY_REPOSITORY = "REPOSITORY";
    public static final String JSON_KEY_PROJECTS = "PROJECTS";
    public static final String JSON_KEY_PROJECT = "PROJECT";
    public static final String JSON_KEY_TIME = "TIME";
    public static final String JSON_KEY_CARDS = "CARDS";
    public static final String JSON_KEY_COLUMNS = "COLUMNS";
    public static final String JSON_NULL = "null";

    private Constants() {
    }
}
