package wit.edu.newsie.data;

import android.os.AsyncTask;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class FacebookQueries {

    private HashMap<String, String> likes = new HashMap<String, String>();
    private HashMap<String, String> reverseLikes = new HashMap<String, String>();
    private List<String> likeNames = new ArrayList<String>();

    public List<String> getLikeNames() {
        return likeNames;
    }

    public HashMap<String, String> getReverseLikes() {
        return reverseLikes;
    }

    public HashMap<String, String> getFacebookLikes(AccessToken accessToken) {
        getLikes(accessToken);
        return likes;
    }

    private void getLikes(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject user, GraphResponse response) {
                if (user != null) {
                    String userId = user.optString("id");
                    new GraphRequest(
                            AccessToken.getCurrentAccessToken(),
                            userId + "/likes",
                            null,
                            HttpMethod.GET,
                            new GraphRequest.Callback() {
                                public void onCompleted(GraphResponse response) {
                                    parseLikes(response.getRawResponse());
                                }
                            }
                    ).executeAsync();
                }
            }
        });
        request.executeAsync();
    }

    private void parseLikes(String json) {
        try {
            JSONObject main = new JSONObject(json);
            JSONArray data = main.getJSONArray("data");
            JSONObject jo;
            String name, id;
            for (int i = 0; i < data.length(); i++) {
                jo = new JSONObject();
                jo = data.getJSONObject(i);
                name = jo.getString("name");
                id = jo.getString("id");
                likes.put(name, id);
                reverseLikes.put(id, name);
                likeNames.add(name);
            }
            JSONObject paging = main.getJSONObject("paging");
            String nextText = paging.getString("next");
            getNext(nextText);
        } catch (JSONException e) {
            Log.e("JSONException", "" + e.getMessage());
            return;
        }

    }

    private void getNext(final String next) {
        GetNextLikesTask gnlt = new GetNextLikesTask();
        try {
            parseLikes(gnlt.execute(next).get());
        } catch (InterruptedException e) {
            Log.e("InterruptedException", "" + e.getMessage());
        } catch (ExecutionException e) {
            Log.e("ExecutionException", "" + e.getMessage());
        }
    }

    private class GetNextLikesTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return getNextLikes(params[0].toString());
        }

        protected void onPostExecute(Double result) {}

        protected void onProgressUpdate(Integer... progress) {}

        public String getNextLikes(String nextUrl) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(nextUrl);

            try {
                HttpResponse response = httpclient.execute(httpget);
                String json = EntityUtils.toString(response.getEntity());
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
