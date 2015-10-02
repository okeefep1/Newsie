package wit.edu.newsie.data;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import wit.edu.newsie.OnTaskCompleted;
import wit.edu.newsie.objects.FeedObject;
import wit.edu.newsie.objects.InstagramFollow;


public class InstagramQueries {

    private HashMap<String, InstagramFollow> follows = new HashMap<String, InstagramFollow>();
    private HashMap<String, InstagramFollow> followsIds = new HashMap<String, InstagramFollow>();
    private List<String> followsNames = new ArrayList<String>();

    public HashMap<String, InstagramFollow> getUserFollows(String userId, String accessToken) {

        GetFollowsTask gft = new GetFollowsTask();
        try {
            String jsonResponse = gft.execute(userId, accessToken).get();
            parseFollows(jsonResponse);
            return follows;
        } catch (InterruptedException e) {
            Log.e("InterruptedException", e.getMessage());
            return null;
        } catch (ExecutionException e) {
            Log.e("ExecutionException", e.getMessage());
            return null;
        }
    }

    public boolean getInstagramPosts(String userId, String accessToken, OnTaskCompleted otc) {

        GetUserFeedTask guft = new GetUserFeedTask(otc);
        try {
            String jsonResponse = guft.execute(userId, accessToken).get();
            return true;
        } catch (InterruptedException e) {
            Log.e("FeedInterruptedExc", e.getMessage());
            return false;
        } catch (ExecutionException e) {
            Log.e("FeedExecutionException", e.getMessage());
            return false;
        }
    }

    private void parseFeed(String json, List<FeedObject> posts) {
        JSONObject jo;
        JSONArray data = new JSONArray();
        try {
            jo = new JSONObject(json);
            data = jo.getJSONArray("data");
        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
            return;
        }
        JSONObject dataItem = new JSONObject();
        FeedObject fo;
        for (int i = 0; i < data.length(); i++) {
            fo = new FeedObject();
            try {
                dataItem = data.getJSONObject(i);
                JSONObject user = dataItem.getJSONObject("user");
                fo.setName(user.getString("username"));
                fo.setSource("instagram");
                String type = dataItem.getString("type");
                fo.setType(type);
                if (type.equals("image")) {
                    JSONObject images = dataItem.getJSONObject("images");
                    JSONObject image = images.getJSONObject("standard_resolution");
                    fo.setPicUrl(image.getString("url"));
                } else if (type.equals("video")) {
                    JSONObject videos = dataItem.getJSONObject("videos");
                    JSONObject video = videos.getJSONObject("standard_resolution");
                    fo.setVideoUrl(video.getString("url"));
                }
                Long createdtime = Long.parseLong(dataItem.getString("created_time")) * 1000;
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(createdtime);
                fo.setDatetime(cal.getTime());
                Calendar now = Calendar.getInstance();
                long timeRemaining = now.getTimeInMillis() - cal.getTimeInMillis();
                long days = TimeUnit.MILLISECONDS.toDays(timeRemaining);
                timeRemaining = (timeRemaining % (1000*60*60*24));
                long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining);
                timeRemaining = (timeRemaining % (1000*60*60));
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining);
                String timeString;
                if (days > 0) {
                    timeString = "" + cal.getTime();
                } else if (hours > 0) {
                    timeString = hours + " hours ago";
                } else {
                    timeString = minutes + " minutes ago";
                }
                fo.setTime(timeString);
            } catch (JSONException e) {
                Log.e("JSONException", e.getMessage());
            }
            try {
                JSONObject caption = dataItem.getJSONObject("caption");
                fo.setContent(caption.getString("text"));
            } catch (JSONException e) {
                fo.setContent(null);
            }
            posts.add(fo);
        }
    }

    private void parseFollows(String json) {
        JSONObject pagination = new JSONObject();
        JSONObject jo = new JSONObject();
        try {
            jo = new JSONObject(json);
            JSONArray data = jo.getJSONArray("data");
            InstagramFollow instFollow;
            for (int i = 0; i < data.length(); i++) {
                instFollow = new InstagramFollow();
                JSONObject user = data.getJSONObject(i);
                instFollow.setUsername(user.getString("username"));
                instFollow.setPicUrl(user.getString("profile_picture"));
                instFollow.setInstagramId(user.getString("id"));
                instFollow.setFullName(user.getString("full_name"));
                follows.put(instFollow.getFullName(), instFollow);
                followsIds.put(instFollow.getInstagramId(), instFollow);
                followsNames.add(instFollow.getFullName());
            }

        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
        }

        try {
            pagination = jo.getJSONObject("pagination");
            getNextFollows(pagination.getString("next_url"));
        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
        }
    }

    public List<String> getFollowsNames() {
        return this.followsNames;
    }

    public HashMap<String, InstagramFollow> getFollowsIds() { return this.followsIds; }

    private void getNextFollows(String nextUrl) {
        GetNextFollowsTask gnft = new GetNextFollowsTask();
        try {
            parseFollows(gnft.execute(nextUrl).get());
        } catch (InterruptedException e) {
            Log.e("NextInterrupted", e.getMessage());
        } catch (ExecutionException e) {
            Log.e("NextExecutionException", e.getMessage());
        }
    }

    private class GetNextFollowsTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return getNextFollows(params[0].toString());
        }

        protected void onPostExecute(Double result) {}

        protected void onProgressUpdate(Integer... progress) {}

        public String getNextFollows(String nextUrl) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(nextUrl);

            try {
                HttpResponse response = httpclient.execute(httpget);
                return EntityUtils.toString(response.getEntity());
            } catch (ClientProtocolException e) {
                Log.e("ClientProtocolException", e.getMessage());
                return null;
            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
                return null;
            }
        }
    }

    private class GetFollowsTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return getFollows(params[0].toString(), params[1].toString());
        }

        protected void onPostExecute(Double result) {}

        protected void onProgressUpdate(Integer... progress) {}

        public String getFollows(String userId, String accessToken) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("https://api.instagram.com/v1/users/" + userId + "/follows?access_token=" + accessToken);

            try {
                HttpResponse response = httpclient.execute(httpget);
                return EntityUtils.toString(response.getEntity());
            } catch (ClientProtocolException e) {
                Log.e("ClientProtocolException", e.getMessage());
                return null;
            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
                return null;
            }
        }
    }

    private class GetUserFeedTask extends AsyncTask<String, Integer, String> {

        private OnTaskCompleted listener;
        private List<FeedObject> instagramPosts = new ArrayList<FeedObject>();
        private String json;

        public GetUserFeedTask(OnTaskCompleted listener){
            this.listener = listener;
        }

        @Override
        protected void onPostExecute(String result){
            Log.v("Hey", "InPostExecute");
            parseFeed(json, instagramPosts);
            Log.v("NumInstaPosts", "" + instagramPosts.size());
            listener.onTaskCompleted(instagramPosts);
        }

        @Override
        protected String doInBackground(String... params) {
            return getFeed(params[0].toString(), params[1].toString());
        }

        protected void onProgressUpdate(Integer... progress) {}

        public String getFeed(String userId, String accessToken) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("https://api.instagram.com/v1/users/" + userId + "/media/recent/?access_token=" + accessToken);

            try {
                HttpResponse response = httpclient.execute(httpget);
                json = EntityUtils.toString(response.getEntity());
                return json;
            } catch (ClientProtocolException e) {
                Log.e("ClientProtocolException", e.getMessage());
                return null;
            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
                return null;
            }
        }
    }
}
