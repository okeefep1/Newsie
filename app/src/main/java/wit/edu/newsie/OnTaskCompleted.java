package wit.edu.newsie;

import java.util.List;

import wit.edu.newsie.objects.FeedObject;


public interface OnTaskCompleted {
    void onTaskCompleted(List<FeedObject> list);
}
