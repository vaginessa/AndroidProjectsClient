package com.tpb.projects.data;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.tpb.projects.data.auth.models.Project;
import com.tpb.projects.data.auth.models.Repository;
import com.tpb.projects.data.auth.models.User;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by theo on 14/12/16.
 */

public class Loader extends APIHandler {
    private static final String TAG = Loader.class.getSimpleName();

    public Loader(Context context) {
        super(context);
    }


    public void loadRepositories(RepositoriesLoader loader, String user) {
        AndroidNetworking.get(GIT_BASE + "users/" + user + "/repos")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray jsa) {
                        try {
                            final Repository[] repos = new Repository[jsa.length()];
                            for(int i = 0; i < repos.length; i++) {
                                repos[i] = Repository.parse(jsa.getJSONObject(i));
                            }
                            Log.i(TAG, "onResponse: successfully parsed repos");
                            if(loader != null) loader.reposLoaded(repos);
                        } catch(JSONException jse) {
                            Log.i(TAG, "onResponse: " + jsa.toString());
                            Log.e(TAG, "onResponse: ", jse);
                            Logging.largeDebugDump(TAG, jse.getMessage());
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    public void loadRepository(RepositoryLoader loader, String fullRepoName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        final Repository repo = Repository.parse(response);
                        if(loader != null) loader.repoLoaded(repo);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });

    }

    public void loadRepositories(RepositoriesLoader loader) {
        AndroidNetworking.get(GIT_BASE + "user/repos")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray jsa) {
                        try {
                            final Repository[] repos = new Repository[jsa.length()];
                            for(int i = 0; i < repos.length; i++) {
                                repos[i] = Repository.parse(jsa.getJSONObject(i));
                            }
                            Log.i(TAG, "onResponse: successfully parsed repos");
                            if(loader != null) loader.reposLoaded(repos);
                        } catch(JSONException jse) {
                            Log.i(TAG, "onResponse: " + jsa.toString());
                            Log.e(TAG, "onResponse: ", jse);
                            Logging.largeDebugDump(TAG, jse.getMessage());
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: User repos" + anError.getErrorBody());
                    }
                });
    }

    public void loadReadMe(ReadMeLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullName + "/readme")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final String readme = Data.base64Decode(response.getString("content"));
                            Log.i(TAG, "onResponse: " + readme);
                            if(loader != null) loader.readMeLoaded(readme);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, "onError: ", anError);
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    public void loadProjects(ProjectsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullName + "/projects")
                .addHeaders(PREVIEW_API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            final Project[] projects = new Project[response.length()];
                            for(int i = 0; i < response.length(); i++) {
                                projects[i] = Project.parse(response.getJSONObject(i));
                            }
                            if(loader != null) loader.projectsLoaded(projects);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ",jse );
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, "onError: ", anError);
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    public void loadCollaborators(final CollaboratorsLoader loader, String repoFullName) {
        AndroidNetworking.get(GIT_BASE + "repos/" + repoFullName + "/collaborators")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final User[] collabs = new User[response.length()];
                        for(int i = 0; i < collabs.length; i++) {
                            try {
                                collabs[i] = User.parse(response.getJSONObject(i));
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }
                        if(loader != null) loader.collaboratorsLoaded(collabs);
                        Log.i(TAG, "onResponse: Collaborators"+ Arrays.toString(collabs));
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: Collaborators" + anError.getErrorBody());
                    }
                });
    }

    public void checkAccess(AccessCheckListener listener, String login, String repoFullname) {
        loadCollaborators(new CollaboratorsLoader() {
            @Override
            public void collaboratorsLoaded(User[] collaborators) {
                for(User u : collaborators) {
                    Log.i(TAG, "collaboratorsLoaded: Comparing " + login + " to " + u.getLogin());
                    if(login.equals(u.getLogin()) && listener != null) {
                        listener.checkComplete(true);
                        return;
                    }
                }
                if(listener != null) listener.checkComplete(false);
            }

            @Override
            public void loadError() {
                if(listener != null) listener.checkError();
            }
        }, repoFullname);
    }

    public enum LoadError {
        NOT_FOUND, UNKNOWN
    }

    public interface RepositoriesLoader {

        void reposLoaded(Repository[] repos);

        void loadError();

    }

    public interface RepositoryLoader {

        void repoLoaded(Repository repo);

        void loadError();
    }

    public interface ReadMeLoader {

        void readMeLoaded(String readMe);

        void loadError();

    }

    public interface ProjectLoader {

        void projectLoaded(Project project);
    }

    public interface ProjectsLoader {

        void projectsLoaded(Project[] projects);

    }

    public interface CollaboratorsLoader {

        void collaboratorsLoaded(User[] collaborators);

        void loadError();

    }

    public interface AccessCheckListener {

        void checkComplete(boolean canAccess);

        void checkError();

    }

}
