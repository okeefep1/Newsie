package wit.edu.newsie;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


public class FragmentMyTopics extends Fragment {

    private ListView lvTopics;
    private ArrayAdapter<String> listAdapter ;
    private Button btnNewTopic;
    private ParseUser currentUser;
    private List<String> topicIds;
    private List<String> topicList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.fragment_my_topics, container, false);

        currentUser = ParseUser.getCurrentUser();
        lvTopics = (ListView) v.findViewById(R.id.lvTopics);
        btnNewTopic = (Button) v.findViewById(R.id.btnNewTopic);
        btnNewTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), NewTopicActivity.class);
                startActivity(i);
            }
        });

        lvTopics.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getActivity(), TopicActivity.class);
                i.putExtra("topic_id", topicIds.get(position));
                SharedPreferences sp = getActivity().getSharedPreferences(getResources().getText(R.string.app_name).toString(), Context.MODE_PRIVATE);
                SharedPreferences.Editor Ed = sp.edit();
                Ed.putString("current_topic_id", topicIds.get(position));
                Ed.commit();
                i.putExtra("topic_title", topicList.get(position));
                startActivity(i);
            }
        });

        return v;
    }

    public void updateTopicList() {
        ParseQuery<ParseObject> userTopicQuery = ParseQuery.getQuery("Topic");
        userTopicQuery.whereEqualTo("userId", currentUser.getObjectId());
        topicList = new ArrayList<String>();
        topicIds = new ArrayList<String>();
        try {
            List<ParseObject> userTopics = userTopicQuery.find();
            for (ParseObject t : userTopics) {
                topicList.add(t.getString("title"));
                topicIds.add(t.getObjectId());
            }
        } catch (ParseException e) {
            Toast.makeText(getActivity().getApplicationContext(), "Error finding Topics", Toast.LENGTH_SHORT).show();
        }
        listAdapter = new ArrayAdapter<String>(getActivity(), R.layout.row_item, topicList);
        lvTopics.setAdapter( listAdapter );
    }

    @Override
    public void onResume() {
        updateTopicList();
        super.onResume();
    }
}
