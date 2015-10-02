package wit.edu.newsie;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import br.com.dina.oauth.instagram.InstagramApp;
import wit.edu.newsie.data.InstagramQueries;
import wit.edu.newsie.data.TwitterQueries;
import wit.edu.newsie.objects.FacebookLike;
import wit.edu.newsie.objects.FeedObject;
import wit.edu.newsie.objects.InstagramFollow;
import wit.edu.newsie.objects.TwitterFollow;

public class TopicActivity extends AppCompatActivity {

    private String topicId, topicTitle;
    private List<FacebookLike> topicFbLikes;
    private List<InstagramFollow> topicInstaFollows;
    private List<TwitterFollow> topicTwitFollows;
    private List<ParseObject> listParseObjectFbLikes, listParseObjectInstaFollows, listParseObjectTwitFollows;
    private ListView lvTopicFeed;
    private List<FeedObject> feedObjects;
    private List<String> names;
    private int numPosts = 0;
    private int postsProcessed = 0;
    private int instaPeopleProcessed = 0;
    private int twitPeopleProcessed = 0;
    private InstagramApp mApp;
    private int tasksCompleted = 0;
    private TwitterSession twitterSession;
    private String token, secret;
    private boolean foundInvalidLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lvTopicFeed = (ListView) findViewById(R.id.lvTopicFeed);

        names = new ArrayList<String>();
        feedObjects = new ArrayList<FeedObject>();
        topicFbLikes = new ArrayList<FacebookLike>();
        topicInstaFollows = new ArrayList<InstagramFollow>();
        topicTwitFollows = new ArrayList<TwitterFollow>();
        Intent i = getIntent();
        topicId = i.getStringExtra("topic_id");
        topicTitle = i.getStringExtra("topic_title");
        getSupportActionBar().setTitle(topicTitle);

        checkCheckboxes();
    }

    private void checkCheckboxes() {
        SharedPreferences sp = getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
        String hasFb = sp.getString("has_facebook", null);
        String hasIntsa = sp.getString("has_instagram", null);
        String hasTwitter = sp.getString("has_twitter", null);
        if (hasFb == null || hasFb.equals("true")) {
            getTopicFbLikes();
        } else if (hasFb.equals("false")) {
            tasksCompleted++;
            if (tasksCompleted == 3) {
                setAdapter();
            }
        }
        if (hasIntsa == null || hasIntsa.equals("true")) {
            instagramPrep();
        } else if (hasIntsa.equals("false")) {
            tasksCompleted++;
            if (tasksCompleted == 3) {
                setAdapter();
            }
        }
        if (hasTwitter == null || hasTwitter.equals("true")) {
            getTopicTwitFollows();
        } else if (hasTwitter.equals("false")) {
            tasksCompleted++;
            if (tasksCompleted == 3) {
                setAdapter();
            }
        }
    }

    private void getTopicTwitFollows() {
        twitterPrep();
        ParseQuery query = ParseQuery.getQuery("TwitterFollow");
        query.whereEqualTo("topicId", topicId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> follows, ParseException e) {
                if (e == null) {
                    listParseObjectTwitFollows = follows;
                    TwitterFollow twitterFollow;
                    for (ParseObject po : follows) {
                        twitterFollow = new TwitterFollow();
                        twitterFollow.setId(po.getObjectId());
                        twitterFollow.setTwitterId("" + po.getNumber("twitterId"));
                        twitterFollow.setTopicId(po.getString("topicId"));
                        topicTwitFollows.add(twitterFollow);
                    }
                    Toast.makeText(getApplicationContext(), "Got Twitter Follows successfully", Toast.LENGTH_SHORT).show();
                    loadTwitterPagePosts();
                } else {
                    Toast.makeText(getApplicationContext(), "Error getting Twitter Follows", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void twitterPrep() {
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session != null) {
            twitterSession = session;
            TwitterAuthToken authToken = session.getAuthToken();
            token = authToken.token;
            secret = authToken.secret;
        } else {
            if (!foundInvalidLogin) {
                foundInvalidLogin = true;
                Intent i = new Intent(TopicActivity.this, AccountsActivity.class);
                startActivity(i);
                finish();
            }
            return;
        }
    }

    private void instagramPrep() {
        mApp = new InstagramApp(this, AppData.INSTAGRAM_CLIENT_ID, AppData.INSTAGRAM_CLIENT_SECRET, AppData.INSTAGRAM_CALLBACK_URL);
        mApp.setListener(listener);
        if (!mApp.hasAccessToken()) {
            if (!foundInvalidLogin) {
                foundInvalidLogin = true;
                Intent i = new Intent(TopicActivity.this, AccountsActivity.class);
                startActivity(i);
                finish();
            }
            return;
        } else {
            getTopicInstaFollows();
        }
    }

    InstagramApp.OAuthAuthenticationListener listener = new InstagramApp.OAuthAuthenticationListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(TopicActivity.this, "Connected as " + mApp.getUserName(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFail(String error) {
            Toast.makeText(TopicActivity.this, error, Toast.LENGTH_SHORT).show();
        }
    };

    private void getTopicInstaFollows() {
        ParseQuery query = ParseQuery.getQuery("InstagramFollow");
        query.whereEqualTo("topicId", topicId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> follows, ParseException e) {
                if (e == null) {
                    listParseObjectInstaFollows = follows;
                    InstagramFollow instaFollow;
                    for (ParseObject po : follows) {
                        instaFollow = new InstagramFollow();
                        instaFollow.setId(po.getObjectId());
                        instaFollow.setInstagramId(po.getString("instagramId"));
                        instaFollow.setTopicId(po.getString("topicId"));
                        topicInstaFollows.add(instaFollow);
                    }
                    Toast.makeText(getApplicationContext(), "Got Instagram Follows successfully", Toast.LENGTH_SHORT).show();
                    loadInstagramPagePosts();
                } else {
                    Toast.makeText(getApplicationContext(), "Error getting Instagram Follows", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getTopicFbLikes() {
        ParseQuery query = ParseQuery.getQuery("FacebookLikes");
        query.whereEqualTo("topicId", topicId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> likes, ParseException e) {
                if (e == null) {
                    listParseObjectFbLikes = likes;
                    FacebookLike fl;
                    for (ParseObject po : likes) {
                        fl = new FacebookLike();
                        fl.setId(po.getObjectId());
                        fl.setFacebookId(po.getString("facebookId"));
                        fl.setTopicId(po.getString("topicId"));
                        topicFbLikes.add(fl);
                    }
                    Toast.makeText(getApplicationContext(), "Got likes successfully", Toast.LENGTH_SHORT).show();
                    loadPagePosts();
                } else {
                    Toast.makeText(getApplicationContext(), "Error getting likes", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadTwitterPagePosts() {
        final int numTwitPosts = topicTwitFollows.size();
        if (topicTwitFollows.size() > 0) {
            for (TwitterFollow twitFollow : topicTwitFollows) {
                names.add(twitFollow.getUsername());
                TwitterQueries tq = new TwitterQueries();
                OnTaskCompleted otc = new OnTaskCompleted() {
                    @Override
                    public void onTaskCompleted(List<FeedObject> list) {
                        Log.v("NumTwitterPosts", "" + list.size());
                        feedObjects.addAll(list);
                        twitPeopleProcessed++;
                        if (twitPeopleProcessed == numTwitPosts) {
                            tasksCompleted++;
                            if (tasksCompleted == 3) {
                                setAdapter();
                            }
                        }
                    }
                };
                tq.getFriendTimeline(twitFollow.getTwitterId(), token, secret, otc);
            }
        } else {
            tasksCompleted++;
            if (tasksCompleted == 3) {
                setAdapter();
            }
        }
    }

    private void loadInstagramPagePosts() {
        final int numInstaPosts = topicInstaFollows.size();
        if (topicInstaFollows.size() > 0) {
            for (InstagramFollow instaFollow : topicInstaFollows) {
                names.add(instaFollow.getUsername());
                InstagramQueries iq = new InstagramQueries();
                OnTaskCompleted otc = new OnTaskCompleted() {
                    @Override
                    public void onTaskCompleted(List<FeedObject> list) {
                        feedObjects.addAll(list);
                        instaPeopleProcessed++;
                        Log.v("PostProcessed", "" + instaPeopleProcessed);
                        if (instaPeopleProcessed == numInstaPosts) {
                            tasksCompleted++;
                            if (tasksCompleted == 3) {
                                setAdapter();
                            }
                        }
                    }
                };
                iq.getInstagramPosts(instaFollow.getInstagramId(), mApp.getAccessToken(), otc);
            }
        } else {
            tasksCompleted++;
            if (tasksCompleted == 3) {
                setAdapter();
            }
        }
    }

    private void loadPagePosts() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            if (!foundInvalidLogin) {
                foundInvalidLogin = true;
                Intent i = new Intent(TopicActivity.this, AccountsActivity.class);
                startActivity(i);
                finish();
            }
            return;
        }
        numPosts = topicFbLikes.size();
        if (numPosts == 0) {
            tasksCompleted++;
            if (tasksCompleted == 3) {
                setAdapter();
            }
        } else {
            for (FacebookLike fl : topicFbLikes) {
                new GraphRequest(
                        accessToken,
                        fl.getFacebookId() + "/posts/",
                        null,
                        HttpMethod.GET,
                        new GraphRequest.Callback() {
                            public void onCompleted(GraphResponse response) {
                                parseFbLikeJSON(response.getJSONObject());
                                postsProcessed++;
                                if (postsProcessed == numPosts) {
                                    tasksCompleted++;
                                    if (tasksCompleted == 3) {
                                        setAdapter();
                                    }
                                }
                            }
                        }
                ).executeAsync();
            }
        }
    }

    private void setAdapter() {
        sortFeedObjects();
        TopicFeedListAdapter adapter = new TopicFeedListAdapter(TopicActivity.this, feedObjects, names);
        Log.v("NumFeedObjects", "" + feedObjects.size());
        lvTopicFeed.setAdapter(adapter);
    }

    private void sortFeedObjects() {
        Collections.sort(feedObjects, new Comparator<FeedObject>() {
            @Override
            public int compare(FeedObject feedObject, FeedObject feedObject2) {
            return feedObject2.getDatetime().compareTo(feedObject.getDatetime());
            }
        });
    }

    private void parseFbLikeJSON(JSONObject like) {
        JSONArray jsonPosts = new JSONArray();
        try {
            jsonPosts = like.getJSONArray("data");
        } catch (JSONException e) {
            Log.e("JSONException", "Error parsing data JSON");
        }
        Log.v("NumFbPosts", "" + jsonPosts.length());
        for (int i = 0; i < jsonPosts.length(); i++) {
            try {
                FeedObject fo = new FeedObject();
                JSONObject post = jsonPosts.getJSONObject(i);
                JSONObject pageInfo = post.getJSONObject("from");
                String pageName = pageInfo.getString("name");
                String postMsg = post.getString("message");
                String postDate = post.getString("created_time");
                String picUrl = post.getString("picture");
                String type = post.getString("type");
                String objectId = post.getString("object_id");
                fo.setObjectId(objectId);
                if (type.equals("video")) {
                    fo.setVideoUrl(post.getString("source"));
                }
                fo.setPicUrl(picUrl);
                int index = postDate.indexOf("T");
                int plusIndex = postDate.indexOf("+");
                String newTimeString = postDate.substring(0, index) + " " + postDate.substring(index + 1, plusIndex);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(newTimeString));
                fo.setDatetime(sdf.parse(newTimeString));
                cal.add(Calendar.HOUR, -4);
                Calendar now = Calendar.getInstance();
                long timeRemaining = now.getTimeInMillis() - cal.getTimeInMillis();
                long days = TimeUnit.MILLISECONDS.toDays(timeRemaining);
                timeRemaining = (timeRemaining % (1000*60*60*24));
                long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining);
                timeRemaining = (timeRemaining % (1000*60*60));
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining);
                fo.setContent(postMsg);
                names.add(pageName);
                fo.setName(pageName);
                fo.setType(type);
                String timeString;
                if (days > 0) {
                    timeString = "" + cal.getTime();
                } else if (hours > 0) {
                    timeString = hours + " hours ago";
                } else {
                    timeString = minutes + " minutes ago";
                }
                fo.setTime(timeString);
                fo.setSource("facebook");
                feedObjects.add(fo);
            } catch (JSONException e) {
                Log.e("JSONException", "Error parsing JSON of liked posts: " + e.getMessage());
            } catch (java.text.ParseException e) {
                Log.e("DateException", "error parsing date: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_topic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_selections) {
            Intent i = new Intent(TopicActivity.this, TopicSelectionsActivity.class);
            i.putExtra("topic_id", topicId);
            i.putExtra("topic_title", topicTitle);
            startActivity(i);
            return true;
        }
        else if (id == R.id.action_delete) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            ParseObject topic = new ParseObject("Topic");
                            topic.setObjectId(topicId);
                            topic.deleteInBackground(new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Toast.makeText(getApplicationContext(), "Delete successful", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Delete unsuccessful", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            ParseObject.deleteAllInBackground(listParseObjectFbLikes, new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Toast.makeText(getApplicationContext(), "Delete Facebook objects successful", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Delete Facebook objects unsuccessful", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            ParseObject.deleteAllInBackground(listParseObjectInstaFollows, new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Toast.makeText(getApplicationContext(), "Delete Instagram objects successful", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Delete Instagram objects unsuccessful", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            ParseObject.deleteAllInBackground(listParseObjectTwitFollows, new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        Toast.makeText(getApplicationContext(), "Delete Twitter objects successful", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Delete Twitter objects unsuccessful", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.MyDialogTheme));
            builder.setMessage("Are you sure you want to delete " + topicTitle + "?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
            return true;
        }
        else if (id == R.id.action_edit_name) {
            // get prompts.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            View promptView = layoutInflater.inflate(R.layout.edit_topic_name, null);
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(new ContextThemeWrapper(this, R.style.MyDialogTheme));

            // set prompts.xml to be the layout file of the alertdialog builder
            alertDialogBuilder.setView(promptView);
            final EditText input = (EditText) promptView.findViewById(R.id.etNewTopicName);

            // setup a dialog window
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // get user input and set it to result
                            topicTitle = input.getText().toString();
                            if (!topicTitle.isEmpty()) {
                                ParseQuery<ParseObject> query = ParseQuery.getQuery("Topic");

                                // Retrieve the object by id
                                query.getInBackground(topicId, new GetCallback<ParseObject>() {
                                    public void done(ParseObject topic, ParseException e) {
                                        if (e == null) {
                                            topic.put("title", topicTitle);
                                            topic.saveInBackground();
                                        }
                                    }
                                });
                                getSupportActionBar().setTitle(topicTitle);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,	int id) {
                            dialog.cancel();
                        }
                    });

            // create an alert dialog
            android.app.AlertDialog alertD = alertDialogBuilder.create();
            alertD.show();

            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        names = new ArrayList<String>();
        feedObjects = new ArrayList<FeedObject>();
        topicFbLikes = new ArrayList<FacebookLike>();
        topicInstaFollows = new ArrayList<InstagramFollow>();
        topicTwitFollows = new ArrayList<TwitterFollow>();
        numPosts = 0;
        postsProcessed = 0;
        instaPeopleProcessed = 0;
        twitPeopleProcessed = 0;
        tasksCompleted = 0;
        checkCheckboxes();
        super.onResume();
    }
}
