# Connecting to two Elasticsearch clusters with different versions of API from same JVM
This project demostrates how is it possible to use two different API libraries for Elasticsearch on same JVM, connecting to two clusters running different ES versions, read from old cluster
(2.3.0) and populate new cluster (5.6.0).

This is implemented using `maven-shade-plugin` - first, Elasticsearch API with all its dependencies and dependencies of dependencies are shaded, i.e. gathered into uber jar substituting different names for classes - both in jar entries and within references to other classes within classes themselves. This uber jar is then installed into local Maven repo and added as dependency to other Maven project which actually uses two versions of API - old shaded one and new unshaded one.

This makes sure that no conflicts occur (In theory, if some classes use reflection and are shaded, they may instantiate unshaded classes of a wrong version - but in this case it works smoothly).

This implementation does not preserve the IDs of documents, and works only with simple mappings. The main point of this project is to demonstrate a way to use two versions simultaneously.
FYI: for migration of indices, such tools as "elasticsearch migration" plugin and "reindex from remote" API do exist. (But maybe in some contexts custom tool may be needed).

# Running
(replace localhost below with host names where clusters listen - if they aren't both on localhost)

0. Create shaded artifact for Elasticsearch 2.4.0:
```
cd shaded-elasticsearch
mvn install
```
1. Install Elasticsearch 2.4.0 and make sure it uses ports 9200 and 9300
```
cluster.name: oldcluster
node.name: oldes
network.host: localhost
# http.port: 9200
# transport.tcp.port: 9300
```
2. Install Elasticsearch 5.6.0 and make sure it uses ports 10200 and 10300
```
cluster.name: newcluster
node.name: newes
network.host: localhost
http.port: 10200
transport.tcp.port: 10300
```
3. Create simple index on Elasticsearch 2.4.0:

```
curl -XPUT http://localhost:9200/oldindex -d @mapping.json
```

4. Populate it with data:
```
curl -s -XPOST 'http://localhost:9200/oldindex/element/_bulk?pretty&v' --data-binary "@data.txt"
```
5. Create new index on a new cluster:
```
curl -XPUT http://localhost:10200/newindex -d @mapping.json
```

6. Run program
(make sure it uses same repo where shaded ES 2.4.0 with dependencies was installed)
```
cd reindexer
mvn package
java -jar target/reindexer-1.0-SNAPSHOT.jar
```
7. Verify that data is in a new index now:
```
curl -XPOST http://localhost:10200/newindex/_search?pretty | less
```

