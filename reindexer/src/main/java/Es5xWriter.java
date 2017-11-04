import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.stream.Stream;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Es5xWriter {

    TransportClient client;
    private String documentType;

    public Es5xWriter(String destClusterName, String host, int port, String documentType) {
        client = connect(destClusterName, host, port);
        this.documentType = documentType;
        if (client != null) {
            System.out.println("Nodes in new cluster: " + client.connectedNodes());
        } else {
            System.out.println("Failed to connect to destination cluster");
        }
    }

    private TransportClient connect(String destClusterName, String host, int port) {
        try {
            Settings settings = Settings.builder().put("cluster.name", destClusterName).build();
            return new PreBuiltTransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress(
                            InetAddress.getByName(host), port));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    void bulk(Stream<Map<String, Object>> mapStream) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        mapStream.forEach(map -> {
            System.out.println(map.getClass() + ": " + map);
            try {
                XContentBuilder builder = jsonBuilder().startObject();
                // for more complex mappings, more elaborate approach may be needed
                map.entrySet().stream().forEach(entry -> {
                    try {
                        builder.field(entry.getKey(), entry.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                IndexRequestBuilder indexRequestBuilder =
                        client.prepareIndex(documentType, documentType);
                bulkRequest.add(indexRequestBuilder.setSource(builder.endObject()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            System.out.println("There were failures in bulk: " + bulkResponse.buildFailureMessage());
            // process failures by iterating through each bulk response item
        } else {
            System.out.println("All gone well!");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
