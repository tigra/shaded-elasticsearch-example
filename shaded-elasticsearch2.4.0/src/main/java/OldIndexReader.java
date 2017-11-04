import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import java.net.InetSocketAddress;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;


public class OldIndexReader {

    public static final String SRC_INDEX = "trump";

    public static void main(String[] args) {
        TransportClient client = TransportClient.builder()
                .settings(Settings.builder().put("cluster.name", "oldcluster"))
                .build();
        client.addTransportAddress(new InetSocketTransportAddress(
                new InetSocketAddress("192.168.1.100", 9300)));

        QueryBuilder matchAll = matchAllQuery();

        SearchResponse scrollResponse = client.prepareSearch(SRC_INDEX)
                .setScroll(new TimeValue(60000))
                .setQuery(matchAll).setSize(100).execute().actionGet();

        while (true) {
            for (SearchHit hit: scrollResponse.getHits().getHits()) {
                System.out.println(hit);
            }
            scrollResponse = client.prepareSearchScroll(
                    scrollResponse.getScrollId())
                    .setScroll(new TimeValue(60000)).execute().actionGet();
            if (scrollResponse.getHits().getHits().length == 0) {
                System.out.println("That's all folks!");
                break;
            }
        }
    }
}
