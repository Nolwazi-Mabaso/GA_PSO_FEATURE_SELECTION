import java.util.ArrayList;
import java.util.List;

public class NoGrouping {

    public List<List<Integer>> groupFeatures(List<String[]> data) {

        int columns = data.get(0).length;
        int features = columns - 1;

        List<List<Integer>> groups = new ArrayList<>();

        for (int i = 0; i < features; i++) {
            List<Integer> single = new ArrayList<>();
            single.add(i);
            groups.add(single);
        }

        return groups;
    }
}