import java.util.*;
import java.io.*;

public class LoadData {

    private String file;
    private List<String[]> data;
    private String[] header;

    public LoadData(String file) {
        this.file = file;
        this.data = new ArrayList<>();
    }

    public boolean load() {

        File fileObj = new File(file);

        if (!fileObj.exists()) {
            System.out.println("File does not exist.");
            return false;
        }

        try {
            BufferedReader br =
                    new BufferedReader(
                            new FileReader(fileObj));

            String line;
            boolean hasData = false;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty())
                    continue;

                String[] values = line.split(",");

                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }

                if (isFirstLine) {
                    header = values;      
                    isFirstLine = false;
                    continue;              
                }

                hasData = true;
                data.add(values);
            }

            br.close();

            if (!hasData) {
                System.out.println("Error: Empty file.");
                return false;
            }

            System.out.println("Data loaded successfully.");
            System.out.println("Rows loaded: " + data.size());

            return true;

        } catch (IOException e) {
            System.out.println("Error loading file: " + e.getMessage());
            return false;
        }
    }

    public List<String[]> getData() {
        return data;
    }

    public String[] getHeader() {
        return header;
    }
}