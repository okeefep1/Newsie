package wit.edu.newsie;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import twitter4j.User;
import wit.edu.newsie.data.TwitterQueries;
import wit.edu.newsie.objects.TwitterFollow;


public class FragmentTwitterSelections extends Fragment {

    private ListView lvEditTwitSelections;
    private HashMap<String, User> followMap;
    private HashMap<Long, User> followsIds;
    private List<TwitterFollow> topicTwitFollows;
    private List<String> followNames, objectIds;
    private AutoCompleteTextView actvAddEditTwitFollow;
    private Button btnAddEditTwitFollow, btnTwitterDone;
    private String topicId;
    private TwitterSession twitterSession;
    private String token, secret;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_twitter_selections, container, false);
        lvEditTwitSelections = (ListView) v.findViewById(R.id.lvEditTwitSelections);
        actvAddEditTwitFollow = (AutoCompleteTextView) v.findViewById(R.id.actvAddEditTwitFollow);
        btnAddEditTwitFollow = (Button) v.findViewById(R.id.btnAddEditTwitFollow);
        btnTwitterDone = (Button) v.findViewById(R.id.btnTwitterDone);
        followMap = new HashMap<String, User>();
        followsIds = new HashMap<Long, User>();
        topicTwitFollows = new ArrayList<TwitterFollow>();
        followNames = new ArrayList<String>();
        objectIds = new ArrayList<String>();

        SharedPreferences sp = getActivity().getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
        topicId = sp.getString("current_topic_id", null);

        btnAddEditTwitFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String followText = actvAddEditTwitFollow.getText().toString();
                if (!followText.isEmpty()) {
                    Long twitterId = followMap.get(followText).getId();
                    if (twitterId != null) {
                        ParseObject newLike = new ParseObject("TwitterFollow");
                        newLike.put("twitterId", twitterId);
                        newLike.put("topicId", topicId);
                        newLike.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(getActivity().getApplicationContext(), followText + " added to topic", Toast.LENGTH_SHORT).show();
                                    actvAddEditTwitFollow.setText("");
                                }
                                else {
                                    Toast.makeText(getActivity().getApplicationContext(), "Invalid entry, check the name of selected follow.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        return;
                    }
                }
            }
        });

        btnTwitterDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });
        twitterPrep();
        getAllTwitFollows();
        return v;
    }

    private void twitterPrep() {
        TwitterSession session = Twitter.getSessionManager().getActiveSession();
        if (session != null) {
            twitterSession = session;
            TwitterAuthToken authToken = session.getAuthToken();
            token = authToken.token;
            secret = authToken.secret;
        } else {
            /*Intent i = new Intent(getActivity(), AccountsActivity.class);
            startActivity(i);
            getActivity().finish();*/
        }
    }

    private void getAllTwitFollows() {
        TwitterQueries tq = new TwitterQueries();
        tq.getFriendList("" + twitterSession.getUserId(), twitterSession.getUserName(), token, secret);
        followMap = tq.getFollows();
        followsIds = tq.getUserIds();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_dropdown_item_1line, tq.getFriendNames());
        actvAddEditTwitFollow.setAdapter(adapter);
        getSelectedTwitterFollows();
    }

    private void getSelectedTwitterFollows() {
        ParseQuery query = ParseQuery.getQuery("TwitterFollow");
        query.whereEqualTo("topicId", topicId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> follows, ParseException e) {
                if (e == null) {
                    TwitterFollow twitterFollow;
                    for (ParseObject po : follows) {
                        twitterFollow = new TwitterFollow();
                        twitterFollow.setId(po.getObjectId());
                        twitterFollow.setTwitterId("" + po.getNumber("twitterId"));
                        twitterFollow.setTopicId(po.getString("topicId"));
                        twitterFollow.setFullName(followsIds.get(Long.parseLong(twitterFollow.getTwitterId())).getName());
                        followNames.add(twitterFollow.getFullName());
                        objectIds.add(twitterFollow.getId());
                        topicTwitFollows.add(twitterFollow);
                    }
                    EditSelectionLvAdapter adapter = new EditSelectionLvAdapter(getActivity(), followNames, objectIds, "TwitterFollow");
                    lvEditTwitSelections.setAdapter(adapter);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "Error getting follows", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
