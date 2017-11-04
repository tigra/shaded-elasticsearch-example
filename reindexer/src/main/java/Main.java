public class Main {
    public static final String DEST_INDEX = "newindex";

    public static void main(String[] args) {
//        Writer writer = new PrintWriter();
        Writer writer = new IndexWriter("newcluster",
                "192.168.1.100", 10300, "newindex");
        new Reader(writer).read("oldcluster",
                "192.168.1.100", 9300, "oldindex");
    }
}
