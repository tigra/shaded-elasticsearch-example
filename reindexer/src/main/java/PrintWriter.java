import java.util.Map;
import java.util.stream.Stream;

public class PrintWriter extends Writer {
    @Override
    void bulk(Stream<Map<String, Object>> mapStream) {
        mapStream.forEach(System.out::println);
    }
}
