public class ServerStart {
    public static void main(String[] args) {

        Thread serverThread = new Thread(() -> new ChatUI("Server", true));
        serverThread.start();

    }
}
