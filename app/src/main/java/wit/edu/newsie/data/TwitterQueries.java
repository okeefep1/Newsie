package wit.edu.newsie.data;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import twitter4j.ExtendedMediaEntity;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import wit.edu.newsie.OnTaskCompleted;
import wit.edu.newsie.objects.FeedObject;


public class TwitterQueries {

    private static final String TWITTER_KEY = "RgOSLTjECm0VzracTKFhL56dl";
    private static final String TWITTER_SECRET = "mqygrrwihtNamBb6PhMTevyf65fONQpZKR6o4wDKfypmYvzebQ";
    private List<String> friendNames;
    private HashMap<String, User> follows = new HashMap<String, User>();
    private HashMap<Long, User> followsIds = new HashMap<Long, User>();

    public HashMap<String, User> getFriendList(String userId, String userName, String authToken, String authSecret) {
        GetFriendsListTask gflt = new GetFriendsListTask();
        try {
            boolean isGood = gflt.execute(userId, userName, authToken, authSecret).get();
            if (isGood) {
                friendNames = gflt.getNames();
                follows = gflt.getUsers();
                followsIds = gflt.getIds();
                return follows;
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Log.e("InterruptedException", "" + e.getMessage());
            return null;
        } catch (ExecutionException e) {
            Log.e("ExecutionException", "" + e.getMessage());
            return null;
        }
    }

    public boolean getFriendTimeline(String userId, String authToken, String authSecret, OnTaskCompleted otc) {
        GetFriendTimelineTask gftt = new GetFriendTimelineTask(otc);
        try {
            boolean isGood = gftt.execute(userId, authToken, authSecret).get();
            return isGood;
        } catch (InterruptedException e) {
            Log.e("InterruptedException", "" + e.getMessage());
            return false;
        } catch (ExecutionException e) {
            Log.e("ExecutionException", "" + e.getMessage());
            return false;
        }
    }

    public List<String> getFriendNames() {
        return friendNames;
    }

    public HashMap<String, User> getFollows() { return follows; }

    public HashMap<Long, User> getUserIds() { return followsIds; }

    private class GetFriendsListTask extends AsyncTask<String, Integer, Boolean> {

        private HashMap<String, User> users;
        private HashMap<Long, User> ids;
        private List<String> names;

        public HashMap<String, User> getUsers() {
            return users;
        }

        public HashMap<Long, User> getIds() {
            return ids;
        }

        public List<String> getNames() {
            return names;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_SECRET);
            // Access Token
            String access_token = params[2];
            // Access Token Secret
            String access_token_secret = params[3];

            AccessToken accessToken = new AccessToken(access_token, access_token_secret);
            Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
            long cursor = -1;
            PagableResponseList<User> friends;
            names = new ArrayList<String>();
            users = new HashMap<String, User>();
            ids = new HashMap<Long, User>();
            do {
                try {
                    friends = twitter.getFriendsList(params[1], cursor);
                    for (User u : friends) {
                        users.put(u.getName(), u);
                        names.add(u.getName());
                        ids.put(u.getId(), u);
                    }
                } catch (TwitterException e) {
                    Log.e("TwitterException", "" + e.getMessage());
                    return false;
                }
            } while ((cursor = friends.getNextCursor()) != 0);
            return true;
        }
    }

    private class GetFriendTimelineTask extends AsyncTask<String, Integer, Boolean> {

        private List<FeedObject> feedObjects;
        private OnTaskCompleted listener;

        public List<FeedObject> getFeedObjects() {
            return feedObjects;
        }

        public GetFriendTimelineTask(OnTaskCompleted listener) {
            this.listener = listener;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.v("Hey", "InPostExecute");
            listener.onTaskCompleted(feedObjects);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_SECRET);
            // Access Token
            String access_token = params[1];
            // Access Token Secret
            String access_token_secret = params[2];

            AccessToken accessToken = new AccessToken(access_token, access_token_secret);
            Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
            long cursor = -1;
            try {
                List<twitter4j.Status> statuses = twitter.getUserTimeline(Long.parseLong(params[0]));
                feedObjects = new ArrayList<FeedObject>();
                FeedObject fo;
                for (twitter4j.Status s : statuses) {
                    fo = new FeedObject();
                    fo.setName(s.getUser().getName());
                    fo.setContent(s.getText());
                    fo.setDatetime(s.getCreatedAt());
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(fo.getDatetime().getTime());
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
                    fo.setSource("twitter");
                    ExtendedMediaEntity[] eme = s.getExtendedMediaEntities();
                    if (eme.length > 0) {
                        fo.setType(eme[0].getType());
                        if (fo.getType().equals("photo")) {
                            fo.setPicUrl(eme[0].getMediaURL());
                        } else if (fo.getType().equals("video")) {
                            Log.v("GotVideo", eme[0].getType());
                            fo.setVideoUrl(eme[0].getURL());
                            fo.setPicUrl(eme[0].getMediaURL());
                            fo.setType("twitter_video");
                        } else {
                            fo.setType("text");
                        }
                    } else {
                        fo.setType("text");
                    }
                    feedObjects.add(fo);
                }

            } catch (TwitterException e) {
                Log.e("TwitterException", "" + e.getMessage());
                return false;
            }

            return true;
        }
    }
}
