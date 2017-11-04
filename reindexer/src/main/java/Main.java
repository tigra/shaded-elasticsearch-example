public class Main {

    public static void main(String[] args) {
        Es5xWriter writer = new Es5xWriter("newcluster",
                "192.168.1.100", 10300, "newindex");
        new Es2xReader(writer).read("oldcluster",
                "192.168.1.100", 9300, "oldindex");
    }
}
