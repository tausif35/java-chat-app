public class ClientStart {
    public static void main(String[] args) {

        Thread clientThread = new Thread(() -> new ChatUI("Client", false));
        clientThread.start();

    }
}
