package wit.edu.newsie;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

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

import wit.edu.newsie.objects.FacebookLike;

public class FragmentFacebookSelections extends Fragment {

    private ListView lvEditFbSelections;
    private AccessToken accessToken;
    private HashMap<String, String> likeMap, reverseMap;
    private List<FacebookLike> topicFbLikes;
    private List<String> likeNames, objectIds;
    private AutoCompleteTextView actvAddEditLike;
    private Button btnAddEditLike, btnFbDone;
    private String topicId;
    private List<String> names;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_facebook_selections, container, false);
        lvEditFbSelections = (ListView) v.findViewById(R.id.lvEditFbSelections);
        actvAddEditLike = (AutoCompleteTextView) v.findViewById(R.id.actvAddEditLike);
        btnAddEditLike = (Button) v.findViewById(R.id.btnAddEditLike);
        btnFbDone = (Button) v.findViewById(R.id.btnFbDone);
        accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            /*Intent i = new Intent(getActivity(), AccountsActivity.class);
            startActivity(i);
            getActivity().finish();*/
        }
        likeMap = new HashMap<String, String>();
        reverseMap = new HashMap<String, String>();
        topicFbLikes = new ArrayList<FacebookLike>();
        likeNames = new ArrayList<String>();
        objectIds = new ArrayList<String>();
        names = new ArrayList<String>();

        SharedPreferences sp = getActivity().getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
        topicId = sp.getString("current_topic_id", null);

        btnAddEditLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String likeText = actvAddEditLike.getText().toString();
                if (!likeText.isEmpty()) {
                    String likeId = likeMap.get(likeText);
                    if (!likeId.isEmpty()) {
                        ParseObject newLike = new ParseObject("FacebookLikes");
                        newLike.put("facebookId", likeId);
                        newLike.put("topicId", topicId);
                        newLike.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(getActivity().getApplicationContext(), likeText + " added to topic", Toast.LENGTH_SHORT).show();
                                    actvAddEditLike.setText("");
                                }
                                else {
                                    Toast.makeText(getActivity().getApplicationContext(), "Invalid entry, check the name of selected like.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        return;
                    }
                }
            }
        });

        btnFbDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sp = getActivity().getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
                String hasInsta = sp.getString("has_instagram", null);
                String hasTwitter = sp.getString("has_twitter", null);
                if (hasInsta.equals("false")) {
                    if (hasTwitter.equals("false")) {
                        getActivity().finish();
                    } else {
                        getFragmentManager().beginTransaction().replace(R.id.selectionContainer, new FragmentTwitterSelections()).commit();
                    }
                } else {
                    getFragmentManager().beginTransaction().replace(R.id.selectionContainer, new FragmentInstagramSelections()).commit();
                }
            }
        });

        getLikes();
        return v;
    }

    private void getLikes() {
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
                likeMap.put(name, id);
                reverseMap.put(id, name);
                likeNames.add(name);
            }
            JSONObject paging = main.getJSONObject("paging");
            String nextText = paging.getString("next");
            getNext(nextText);
        } catch (JSONException e) {
            Log.e("JSONException", "" + e.getMessage());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_dropdown_item_1line, likeNames);
            actvAddEditLike.setAdapter(adapter);
            getSelectedFbLikes();
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

    private void getSelectedFbLikes() {
        ParseQuery query = ParseQuery.getQuery("FacebookLikes");
        query.whereEqualTo("topicId", topicId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> likes, ParseException e) {
                if (e == null) {
                    FacebookLike fl;
                    for (ParseObject po : likes) {
                        fl = new FacebookLike();
                        fl.setId(po.getObjectId());
                        fl.setFacebookId(po.getString("facebookId"));
                        fl.setTopicId(po.getString("topicId"));
                        fl.setName(reverseMap.get(fl.getFacebookId()));
                        names.add(fl.getName());
                        objectIds.add(fl.getId());
                        topicFbLikes.add(fl);
                    }
                    EditSelectionLvAdapter adapter = new EditSelectionLvAdapter(getActivity(), names, objectIds, "FacebookLikes");
                    lvEditFbSelections.setAdapter(adapter);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "Error getting likes", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
