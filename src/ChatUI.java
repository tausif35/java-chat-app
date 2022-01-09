import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatUI extends JFrame {

    public static final String IP = "localhost";
    public static final int PORT = 1234;
    private final boolean isServer;
    private final ArrayList<FileReceived> fileReceivedArrayList = new ArrayList<>();

    private JPanel mainPanel, chatBox;
    private JButton textButton, fileButton;
    private JTextArea inputBox;
    private JScrollPane chatBoxScroll;
    private JLabel userName;

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private int fileID = 0;

    public ChatUI(String title, boolean isServer) {
        setTitle(title);
        this.isServer = isServer;
        userName.setText(title);

        initialize();
        initListeners();
        receiveMsg();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setPreferredSize(new Dimension(600, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
        chatBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputBox.grabFocus();
    }

    private void createConnection() {
        try {
            socket = isServer ? new ServerSocket(PORT).accept() : new Socket(IP, PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
        }
    }

    private void initListeners() {
        textButton.addActionListener(e -> {
            String text = inputBox.getText().strip();
            if (!text.isBlank()) {
                sendText(text);
            }
        });

        fileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose a file to send");
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                int dialogResult = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to send this file?\n" + fileChooser.getSelectedFile().getName(),
                        "Send file", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    sendFile(fileChooser.getSelectedFile());
                }
            }
        });
    }

    private void receiveMsg() {
        new Thread(() -> {
            try {
                createConnection();

                while (socket.isConnected()) {

                    //Receive boolean for text or file
                    boolean isFile = dataInputStream.readBoolean();

                    //Read the msg (text or file name)
                    int msgBytesLength = dataInputStream.readInt();
                    byte[] msgBytes = new byte[msgBytesLength];

                    if (msgBytesLength > 0) {
                        dataInputStream.readFully(msgBytes, 0, msgBytesLength);

                        if (isFile) {
                            //If the msg was file, read the file content
                            int fileContentLength = dataInputStream.readInt();
                            if (fileContentLength > 0) {
                                byte[] fileContentBytes = new byte[fileContentLength];
                                dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                                fileReceivedArrayList.add(new FileReceived(fileID, new String(msgBytes), fileContentBytes));
                            }

                        }
                        addMsgLabelToUI(true, new String(msgBytes), isFile);

                        if (isFile)
                            fileID++;
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
                closeEverything();
            }
        }).start();
    }

    private void closeEverything() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }

            if (!isServer) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "The server might be down. Try to reconnect?", "Error!", JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {
                    createConnection();
                } else {
                    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                }
            } else {
                JOptionPane.showMessageDialog(null, "Client has left the chat");
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendText(String text) {
        boolean isFile = false;

        try {
            //Send boolean for if the msg is a text or file
            dataOutputStream.writeBoolean(isFile);

            dataOutputStream.writeInt(text.getBytes().length);
            dataOutputStream.write(text.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        inputBox.setText("");
        inputBox.grabFocus();
        addMsgLabelToUI(false, text, false);
    }


    private void sendFile(File file) {
        try {
            boolean isFile = true;

            //Send boolean for if the msg is a text or file
            dataOutputStream.writeBoolean(isFile);

            //Read the file
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
            String fileName = file.getName();

            //Read the file content
            byte[] fileContentBytes = new byte[(int) file.length()];
            fileInputStream.read(fileContentBytes);

            //Send file name
            dataOutputStream.writeInt(fileName.getBytes().length);
            dataOutputStream.write(fileName.getBytes());

            //Send file content
            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            addMsgLabelToUI(false, fileName, isFile);

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void addMsgLabelToUI(boolean isReceived, String text, boolean isFile) {
        text = text.replaceAll("\n", "<br>");
        if (isFile)
            text = "<u><i>" + text + "</i></u>";

        String style = String.format("style=\"width:%f px; word-wrap: break-word; text-align: %s;\"",
                getWidth() * 0.5,
                isReceived ? "left" : "right");

        //Add new msg label to chat box panel
        JLabel label = new JLabel(String.format("<html><div %s><p style=\"color:%s;\">%s</p></div></html>",
                style,
                isReceived ? "rgb(55,150,250)" : "rgb(40,175,90)",
                text));

        label.setFont(new Font("Unispace", Font.PLAIN, 20));
        label.setHorizontalAlignment(isReceived ? SwingConstants.LEFT : SwingConstants.RIGHT);
        label.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (isReceived && isFile) {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setName(String.valueOf(fileID));
            label.addMouseListener(getLabelClickListeners());
        }
        chatBox.add(label);
        chatBox.add(Box.createVerticalStrut(30));
        scrollToNewMsg();
    }

    private MouseListener getLabelClickListeners() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(
                        null,
                        "Do you want to download this file?",
                        "Warning",
                        JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {
                    JLabel label = (JLabel) e.getSource();

                    for (FileReceived fileReceived : fileReceivedArrayList) {
                        if (fileReceived.getFileID() == Integer.parseInt(label.getName())) {
                            try {
                                File file = new File(System.getProperty("user.home") + "/Downloads/" + fileReceived.getFileName());
                                FileOutputStream fileOutputStream = new FileOutputStream(file);
                                fileOutputStream.write(fileReceived.getFileData());
                                fileOutputStream.close();

                                JOptionPane.showMessageDialog(null, file, "File downloaded to", JOptionPane.PLAIN_MESSAGE);

                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
    }

    private void scrollToNewMsg() {
        //Scroll to bottom
        JScrollBar verticalScrollBar = chatBoxScroll.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
        validate();
    }
}