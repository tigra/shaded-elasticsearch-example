import java.util.Map;
import java.util.stream.Stream;

public abstract class Writer {
    abstract void bulk(Stream<Map<String, Object>> mapStream);
}
