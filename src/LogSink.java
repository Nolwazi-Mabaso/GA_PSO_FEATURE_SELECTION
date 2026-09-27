public interface LogSink {
    void log(String message);
    default void progress(int percent) {}
    default void groups(String json) {}
}