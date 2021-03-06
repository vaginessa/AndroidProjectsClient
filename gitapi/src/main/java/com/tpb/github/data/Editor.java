package com.tpb.github.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.tpb.github.data.models.Card;
import com.tpb.github.data.models.Column;
import com.tpb.github.data.models.Comment;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Milestone;
import com.tpb.github.data.models.Project;
import com.tpb.github.data.models.State;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

import static com.androidnetworking.AndroidNetworking.delete;
import static com.androidnetworking.AndroidNetworking.patch;
import static com.androidnetworking.AndroidNetworking.post;
import static com.androidnetworking.AndroidNetworking.put;

/**
 * Created by theo on 18/12/16.
 */

public class Editor extends APIHandler {
    private static final String TAG = Editor.class.getSimpleName();

    private static final String NAME = "name";
    private static final String BODY = "body";
    private static final String NOTE = "note";
    private static final String TITLE = "title";
    private static final String ASSIGNEES = "assignees";
    private static final String LABELS = "labels";

    private static final String STATE = "state";
    private static final String STATE_CLOSED = "closed";
    private static final String STATE_OPEN = "open";

    private static final String CONTENT_TYPE = "content_type";
    private static final String CONTENT_TYPE_ISSUE = "Issue";
    private static final String CONTENT_ID = "content_id";

    private static final String POSITION = "position";
    private static final String POSITION_FIRST = "first";
    private static final String POSITION_AFTER = "after:";
    private static final String POSITION_TOP = "top";
    private static final String COLUMN_ID = "column_id";

    private static final String SEGMENT_THREADS = "/threads";

    private static Editor editor;

    private Editor(Context context) {
        super(context);
    }

    public static Editor getEditor(Context context) {
        if(editor == null) editor = new Editor(context);
        return editor;
    }

    public Editor createProject(@NonNull final CreationListener<Project> listener, String name, String body, String repoFullName) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(NAME, name);
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createProject: ", jse);
        }
        post(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_PROJECTS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(new Project(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateProject(@NonNull final UpdateListener<Project> listener, String name, String body, int id) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NAME, name);
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "updateProjects: ", jse);
        }
        patch(GIT_BASE + SEGMENT_PROJECTS + "/" + id)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Project(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor deleteProject(@NonNull final DeletionListener<Project> listener, final Project project) {
        delete(GIT_BASE + SEGMENT_PROJECTS + "/" + project.getId())
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0 && anError.getErrorBody() == null) {
                            listener.deleted(project);
                        } else {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
        return this;
    }

    public Editor openProject(@NonNull final UpdateListener<Project> listener, int projectId) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(STATE, STATE_OPEN);
        } catch(JSONException ignored) {
        }
        patch(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Project(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor closeProject(@NonNull final UpdateListener<Project> listener, int projectId) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(STATE, STATE_CLOSED);
        } catch(JSONException ignored) {
        }
        patch(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Project(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateColumnName(@NonNull final UpdateListener<Column> listener, int columnId, String newName) {
        final JSONObject obj = new JSONObject();
        // Again, if we use .addBodyParameter("name", newName), GitHub throws a parsing error

        try {
            obj.put(NAME, newName);
        } catch(JSONException jse) {
            Log.e(TAG, "updateColumnName: ", jse);
        }
        patch(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Column(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor addColumn(@NonNull final CreationListener<Column> listener, int projectId, String name) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NAME, name);
        } catch(JSONException jse) {
            Log.e(TAG, "addColumn: ", jse);
        }
        post(GIT_BASE + SEGMENT_PROJECTS + "/" + projectId + SEGMENT_COLUMNS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(new Column(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor moveColumn(@NonNull final UpdateListener<Integer> listener, final int columnId, int dropPositionId, int position) {
        final JSONObject obj = new JSONObject();
        try {
            if(position == 0) {
                obj.put(POSITION, POSITION_FIRST);
            } else {
                obj.put(POSITION, POSITION_AFTER + dropPositionId);
            }
        } catch(JSONException jse) {
            Log.e(TAG, "moveColumn: ", jse);
        }
        post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_MOVES)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(columnId);
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor deleteColumn(@NonNull final DeletionListener<Integer> listener, final int columnId) {
        delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0) {
                            listener.deleted(columnId);
                        } else {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
        return this;
    }

    public Editor createCard(@NonNull final CreationListener<Pair<Integer, Card>> listener, final int columnId, String note) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NOTE, note);
        } catch(JSONException jse) {
            Log.e(TAG, "createCard: ", jse);
        }
        post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(Pair.create(columnId, new Card(response)));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor createCard(@NonNull final CreationListener<Pair<Integer, Card>> listener, final int columnId, int issueId) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(CONTENT_TYPE, CONTENT_TYPE_ISSUE);
            obj.put(CONTENT_ID, issueId);
        } catch(JSONException jse) {
            Log.e(TAG, "createCard: ", jse);
        }
        post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + "/" + columnId + SEGMENT_CARDS)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(Pair.create(columnId, new Card(response)));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateCard(@NonNull final UpdateListener<Card> listener, final int cardId, String note) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(NOTE, note);
        } catch(JSONException jse) {
            Log.e(TAG, "updateCard: ", jse);
        }
        patch(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + cardId)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Card(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor moveCard(@Nullable final UpdateListener<Integer> listener, final int columnId, final int cardId, int afterId) {
        final JSONObject obj = new JSONObject();
        try {
            if(afterId == -1) {
                obj.put(POSITION, POSITION_TOP);
            } else {
                obj.put(POSITION, POSITION_AFTER + afterId);
            }
            if(columnId != -1) obj.put(COLUMN_ID, columnId);
        } catch(JSONException jse) {
            Log.e(TAG, "moveCard: ", jse);
        }
        post(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + cardId + SEGMENT_MOVES)
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.updated(cardId);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor deleteCard(@NonNull final DeletionListener<Card> listener, final Card card) {
        delete(GIT_BASE + SEGMENT_PROJECTS + SEGMENT_COLUMNS + SEGMENT_CARDS + "/" + card
                .getId())
                .addHeaders(PROJECTS_API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0) {
                            listener.deleted(card);
                        } else {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
        return this;
    }

    public Editor createIssue(@NonNull final CreationListener<Issue> listener, String repoFullName, String title, String body, @Nullable String[] assignees, @Nullable String[] labels) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(TITLE, title);
            obj.put(BODY, body);
            if(assignees != null) obj.put(ASSIGNEES, new JSONArray(assignees));
            if(labels != null) obj.put(LABELS, new JSONArray(labels));
        } catch(JSONException jse) {
            Log.e(TAG, "createIssue: ", jse);
        }
        post(GIT_BASE + SEGMENT_REPOS + "/" + repoFullName + SEGMENT_ISSUES)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(new Issue(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor closeIssue(@Nullable final UpdateListener<Issue> listener, String fullRepoName, int issueNumber) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(STATE, STATE_CLOSED);
        } catch(JSONException jse) {
            Log.e(TAG, "closeIssue: ", jse);
        }
        patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.updated(new Issue(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor openIssue(@NonNull final UpdateListener<Issue> listener, String fullRepoName, int issueNumber) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(STATE, STATE_OPEN);
        } catch(JSONException jse) {
            Log.e(TAG, "openIssue: ", jse);
        }
        patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + "/" + issueNumber)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Issue(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateIssue(@NonNull final UpdateListener<Issue> listener, String fullRepoName, Issue issue, @Nullable String[] assignees, @Nullable String[] labels) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(TITLE, issue.getTitle());
            obj.put(BODY, issue.getBody());
            if(assignees != null) obj.put(ASSIGNEES, new JSONArray(assignees));
            if(labels != null) obj.put(LABELS, new JSONArray(labels));
        } catch(JSONException jse) {
            Log.e(TAG, "createIssue: ", jse);
        }
        patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + "/" + issue
                .getNumber())
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Issue(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor createIssueComment(@NonNull final CreationListener<Comment> listener, String fullRepoName, int issueNumber, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createIssueComment: ", jse);
        }
        post(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + "/" + issueNumber + SEGMENT_COMMENTS)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(new Comment(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateIssueComment(@NonNull final UpdateListener<Comment> listener, String fullRepoName, int commentId, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createIssueComment: ", jse);
        }
        patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Comment(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor deleteIssueComment(@NonNull final DeletionListener<Integer> listener, String fullRepoName, final int commentId) {
        delete(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_ISSUES + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0) {
                            listener.deleted(commentId);
                        } else {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
        return this;
    }

    public Editor createCommitComment(@NonNull final CreationListener<Comment> listener, String fullRepoName, String sha, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "createCommitComment: ", jse);
        }
        post(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_COMMITS + "/" + sha + SEGMENT_COMMENTS)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(new Comment(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateCommitComment(@NonNull final UpdateListener<Comment> listener, String fullRepoName, int commentId, String body) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(BODY, body);
        } catch(JSONException jse) {
            Log.e(TAG, "updateCommitComment: ", jse);
        }
        patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Comment(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor deleteCommitComment(@NonNull final DeletionListener<Integer> listener, String fullRepoName, final int commentId) {
        delete(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_COMMENTS + "/" + commentId)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.deletionError(APIError.UNKNOWN);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(anError.getErrorCode() == 0) {
                            listener.deleted(commentId);
                        } else {
                            listener.deletionError(parseError(anError));
                        }
                    }
                });
        return this;
    }

    public Editor starRepo(@Nullable final UpdateListener<Boolean> listener, String fullRepoName) {
        put(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .addHeaders("Content-Length", "0")
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.updated(true);
                        } else {
                            if(listener != null) listener.updated(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(false);
                    }
                });
        return this;
    }

    public Editor unstarRepo(@Nullable final UpdateListener<Boolean> listener, String fullRepoName) {
        delete(GIT_BASE + SEGMENT_USER + SEGMENT_STARRED + "/" + fullRepoName)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            if(listener != null) listener.updated(false);
                        } else {
                            if(listener != null) listener.updated(true);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(true);
                    }
                });
        return this;
    }

    public Editor watchRepo(@Nullable final UpdateListener<Boolean> listener, String fullRepoName) {
        put(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_SUBSCRIPTION)
                .addHeaders(API_AUTH_HEADERS)
                .addPathParameter("subscribed", "true")
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.has("subscribed")) {
                                if(listener != null)
                                    listener.updated(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.updated(false);
                            }
                        } catch(JSONException jse) {
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(false);
                    }
                });
        return this;
    }

    public Editor unwatchRepo(@Nullable final UpdateListener<Boolean> listener, String fullRepoName) {
        put(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_SUBSCRIPTION)
                .addHeaders(API_AUTH_HEADERS)
                .addPathParameter("subscribed", "false")
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.has("subscribed")) {
                                if(listener != null)
                                    listener.updated(response.getBoolean("subscribed"));
                            } else {
                                if(listener != null) listener.updated(true);
                            }
                        } catch(JSONException jse) {
                            listener.updateError(APIError.UNPROCESSABLE);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.updated(true);
                    }
                });
        return this;
    }

    public Editor followUser(@NonNull final UpdateListener<Boolean> listener, String user) {
        put(GIT_BASE + SEGMENT_USER + SEGMENT_FOLLOWING + "/" + user)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            listener.updated(true);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor unfollowUser(@NonNull final UpdateListener<Boolean> listener, String user) {
        delete(GIT_BASE + SEGMENT_USER + SEGMENT_FOLLOWING + "/" + user)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        if(response.code() == 204) {
                            listener.updated(false);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor createMilestone(@NonNull final CreationListener<Milestone> listener, String fullRepoName, @NonNull String title, @Nullable String description, @Nullable String dueOn) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put(TITLE, title);
            if(description != null) obj.put("description", description);
            if(dueOn != null) obj.put("due_on", dueOn);

        } catch(JSONException wtf) {
            Log.wtf(TAG, "Milestone JSONObject", wtf);
        }
        post(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_MILESTONES)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.created(new Milestone(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.creationError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor updateMilestone(@NonNull final UpdateListener<Milestone> listener, String fullRepoName, int number, @Nullable String title, @Nullable String description, @Nullable String dueOn, @Nullable State state) {
        final JSONObject obj = new JSONObject();
        try {
            if(title != null) obj.put(TITLE, title);
            if(description != null) obj.put("description", description);
            if(dueOn != null) obj.put("due_on", dueOn);
            if(state != null) obj.put("state", state.toString().toLowerCase());
        } catch(JSONException wtf) {
            Log.wtf(TAG, "Milestone update JSONObject", wtf);
        }
        patch(GIT_BASE + SEGMENT_REPOS + "/" + fullRepoName + SEGMENT_MILESTONES + "/" + number)
                .addHeaders(API_AUTH_HEADERS)
                .addJSONObjectBody(obj)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        listener.updated(new Milestone(response));
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.updateError(parseError(anError));
                    }
                });
        return this;
    }

    public Editor markNotificationsRead() {
        put(GIT_BASE + SEGMENT_NOTIFICATIONS)
                .addHeaders(API_AUTH_HEADERS)
                .addPathParameter("last_read_at",
                        Util.toISO8061FromMilliseconds(Util.getUTCTimeInMillis())
                )
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        Log.i(TAG, response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, anError.toString());
                    }
                });
        return this;
    }

    public Editor markNotificationThreadRead(long id) {
        patch(GIT_BASE + SEGMENT_NOTIFICATIONS + SEGMENT_THREADS + "/" + id)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        Log.i(TAG, response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e(TAG, anError.toString());
                    }
                });
        return this;
    }

    public interface CreationListener<T> {

        void created(T t);

        void creationError(APIError error);

    }

    public interface DeletionListener<T> {

        void deleted(T t);

        void deletionError(APIError error);

    }

    public interface UpdateListener<T> {

        void updated(T t);

        void updateError(APIError error);


    }

}
