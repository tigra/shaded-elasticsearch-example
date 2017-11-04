import old.org.elasticsearch.action.ActionFuture;
import old.org.elasticsearch.action.search.ClearScrollRequest;
import old.org.elasticsearch.action.search.ClearScrollRequestBuilder;
import old.org.elasticsearch.action.search.ClearScrollResponse;
import old.org.elasticsearch.action.search.SearchResponse;
import old.org.elasticsearch.client.transport.TransportClient;
import old.org.elasticsearch.common.settings.Settings;
import old.org.elasticsearch.common.transport.InetSocketTransportAddress;
import old.org.elasticsearch.common.unit.TimeValue;
import old.org.elasticsearch.index.query.QueryBuilder;
import old.org.elasticsearch.search.SearchHit;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static old.org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class Reader {
    public static final int SCROLL_DELAY = 60000;
    private final Writer writer;

    public Reader(Writer writer) {
        this.writer = writer;
    }

    public void read(String sourceClusterName, String host, int port, String index) {
        TransportClient client = null;
        try {
            client = TransportClient.builder()
                    .settings(Settings.builder()
                            .put("cluster.name", sourceClusterName))
                    .build();
            client.addTransportAddress(new InetSocketTransportAddress(
                    new InetSocketAddress(host, port)));

            QueryBuilder matchAll = matchAllQuery();

            SearchResponse scrollResponse = client.prepareSearch(index)
                    .setScroll(new TimeValue(SCROLL_DELAY))
                    .setQuery(matchAll).setSize(5).execute().actionGet();
            // scroll size is so little only to demonstrate multiple scrolls

            int scrollNo = 0;
            while (true) {
                System.out.println("Reading scroll " + scrollNo + "...");
                SearchHit[] hits = scrollResponse.getHits().getHits();

                // relying on source only (other strategies may be needed in different cases)
                Stream<Map<String, Object>> mapStream =
                        Arrays.stream(hits).map(SearchHit::getSource);
                writer.bulk(mapStream);

                scrollResponse = client.prepareSearchScroll(
                        scrollResponse.getScrollId())
                        .setScroll(new TimeValue(SCROLL_DELAY)).execute().actionGet();
                if (scrollResponse.getHits().getHits().length == 0) {
                    System.out.println("Clearing scroll...");

                    ClearScrollResponse response = client.prepareClearScroll()
                            .addScrollId(scrollResponse.getScrollId())
                            .execute().actionGet();
                    if (response.isSucceeded()) {
                        System.out.println("Freed scrolls: " + response.getNumFreed());
                    } else {
                        System.out.println("Failed clearing scrolls: " + response);
                    }

                    System.out.println("That's all folks!");
                    break;
                }
                scrollNo++;
            }
        } finally {
            if (client != null) {
                System.out.println("Closing reader...");
                client.close();
            }
        }
    }

}
