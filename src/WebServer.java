import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class WebServer {

    private static boolean running = false;

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // ---- static files ----
        server.createContext("/",          ex -> serveFile(ex, "web/index.html", "text/html"));
        server.createContext("/style.css", ex -> serveFile(ex, "web/style.css",  "text/css"));
        server.createContext("/app.js",    ex -> serveFile(ex, "web/app.js",    "application/javascript"));

        // ---- /run : start pipeline, stream logs via SSE ----
        server.createContext("/run", WebServer::handleRun);

        server.createContext("/pick-file", WebServer::handlePickFile);

        server.start();
        System.out.println("===============================================");
        System.out.println("  Server running at http://localhost:8080");
        System.out.println("===============================================");
    }

    // ================================================================
    //  STATIC FILE SERVING
    // ================================================================
    private static void serveFile(HttpExchange ex, String path, String contentType) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(200, bytes.length);
            OutputStream os = ex.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            try {
                ex.sendResponseHeaders(404, -1);
            } catch (Exception ignored) {}
        }
    }

    // ================================================================
    //  /run ENDPOINT — SSE STREAM
    // ================================================================
    private static void handleRun(HttpExchange ex) {
        try {
            // ---- parse query string ----
            Map<String, String> params = parseQuery(ex.getRequestURI().getQuery());

            String datasetPath  = params.getOrDefault("dataset", "data/dataset.csv");
            String classifiers  = params.getOrDefault("classifiers", "KNN,SVM,NB");

            List<String> selectedClassifiers = List.of(classifiers.split(","));

            // ---- SSE headers ----
            ex.getResponseHeaders().set("Content-Type", "text/event-stream");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.getResponseHeaders().set("Connection", "keep-alive");
            ex.sendResponseHeaders(200, 0);

            OutputStream os = ex.getResponseBody();
            PrintStream ps = new PrintStream(os, true, "UTF-8");

            // ---- SSE log sink ----
            LogSink sink = new LogSink() {
                @Override
                public void log(String message) {
                    send(ps, "log", escape(message));
                }
                @Override
                public void progress(int percent) {
                    send(ps, "progress", String.valueOf(percent));
                }
                @Override
                public void groups(String json) {
                    send(ps, "groups", json);
                }
            };

            // ---- prevent overlapping runs ----
            if (running) {
                send(ps, "log", "A run is already in progress. Please wait.");
                send(ps, "done", "");
                ps.close();
                return;
            }

            // ---- validate dataset path ----
            if (!Files.exists(Paths.get(datasetPath))) {
                send(ps, "log", "ERROR: Dataset not found at: " + datasetPath);
                send(ps, "done", "");
                ps.close();
                return;
            }

            // ---- validate classifier selection ----
            if (selectedClassifiers.isEmpty()) {
                send(ps, "log", "ERROR: No classifier selected.");
                send(ps, "done", "");
                ps.close();
                return;
            }

            running = true;

            try {
                send(ps, "log", "Using dataset: " + datasetPath);
                send(ps, "log", "Running classifiers: " + selectedClassifiers);

                Runner runner = new Runner(datasetPath, sink, selectedClassifiers);
                runner.run();

                // ---- send pipeline-level info ----
                String pipelineJson = "{"
                        + "\"totalFeatures\":" + runner.totalOriginalFeatures + ","
                        + "\"numGroups\":" + runner.numGroups
                        + "}";
                send(ps, "pipeline", pipelineJson);

                // ---- send final results as JSON ----
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < runner.results.size(); i++) {
                    Runner.Result r = runner.results.get(i);
                    if (i > 0) json.append(",");
                    json.append("{")
                        .append("\"classifier\":\"").append(r.classifierName).append("\",")
                        .append("\"gaF1\":").append(r.gaEval.calculateF1Score()).append(",")
                        .append("\"gaGMean\":").append(r.gaEval.calculateGMean()).append(",")
                        .append("\"gaPrecision\":").append(r.gaEval.calculatePrecision()).append(",")
                        .append("\"gaRecall\":").append(r.gaEval.calculateRecall()).append(",")
                        .append("\"psoF1\":").append(r.psoEval.calculateF1Score()).append(",")
                        .append("\"psoGMean\":").append(r.psoEval.calculateGMean()).append(",")
                        .append("\"psoPrecision\":").append(r.psoEval.calculatePrecision()).append(",")
                        .append("\"psoRecall\":").append(r.psoEval.calculateRecall()).append(",")
                        .append("\"gaFeatures\":").append(r.bestGaFeatures.size()).append(",")
                        .append("\"psoFeatures\":").append(r.bestPsoFeatures.size()).append(",")
                        .append("\"gaDurationMs\":").append(r.gaDurationMs).append(",")
                        .append("\"psoDurationMs\":").append(r.psoDurationMs)
                        .append("}");
                }
                json.append("]");

                send(ps, "results", json.toString());
                // ---- send baseline results ----
StringBuilder blJson = new StringBuilder("[");
for (int i = 0; i < runner.baselineResults.size(); i++) {
    Runner.BaselineResult b = runner.baselineResults.get(i);
    if (i > 0) blJson.append(",");
    blJson.append("{")
        .append("\"classifier\":\"").append(b.classifierName).append("\",")
        .append("\"method\":\"All-Features\",")
        .append("\"f1\":").append(b.eval.calculateF1Score()).append(",")
        .append("\"gmean\":").append(b.eval.calculateGMean()).append(",")
        .append("\"precision\":").append(b.eval.calculatePrecision()).append(",")
        .append("\"recall\":").append(b.eval.calculateRecall()).append(",")
        .append("\"numFeatures\":").append(b.numFeatures).append(",")
        .append("\"durationMs\":").append(b.durationMs)
        .append("}");
}
blJson.append("]");

send(ps, "baseline", blJson.toString());
                send(ps, "done", "");

            } catch (Exception e) {
                send(ps, "log", "ERROR: " + escape(e.getMessage()));
                e.printStackTrace();
                send(ps, "done", "");
            } finally {
                running = false;
                ps.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handlePickFile(HttpExchange ex) {
    try {
        AtomicReference<File> chosen = new AtomicReference<>(null);

        // Must run on Swing thread
        SwingUtilities.invokeAndWait(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select dataset CSV file");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "CSV files", "csv", "txt", "data"));

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                chosen.set(chooser.getSelectedFile());
            }
        });

        String response;
        if (chosen.get() == null) {
            response = "{\"path\":null}";
        } else {
            String path = chosen.get().getAbsolutePath().replace("\\", "/");
            response = "{\"path\":\"" + path + "\"}";
        }

        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, response.length());
        OutputStream os = ex.getResponseBody();
        os.write(response.getBytes());
        os.close();

    } catch (Exception e) {
        e.printStackTrace();
        try { ex.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
    }
}

    // ================================================================
    //  HELPERS
    // ================================================================
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;

        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;

            String key   = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    private static void send(PrintStream ps, String event, String data) {
        ps.print("event: " + event + "\n");
        for (String line : data.split("\n", -1)) {
            ps.print("data: " + line + "\n");
        }
        ps.print("\n");
        ps.flush();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }
}