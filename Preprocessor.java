import java.util.*;

public class Preprocessor {

        private List<String[]> data;
        private List<List<Integer>> groups;

        public Preprocessor(List<String[]> data) {
                this.data = data;
        }

        public List<String[]> preprocess() {

                RemoveDuplicates rd = new RemoveDuplicates();

                data = rd.remove(data);

                HandleMissingValues hm = new HandleMissingValues();

                data = hm.clean(data);

                Normalize norm = new Normalize();
                data = norm.scale(data);
                
                PearsonCorrelation pc = new PearsonCorrelation(0.8);
                groups = pc.groupFeatures(data);

                return data;
        }

        public List<List<Integer>> getGroups() {
                return groups;
        }

}