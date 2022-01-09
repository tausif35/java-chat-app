public class FileReceived {
    int fileID;
    String fileName;
    byte[] fileData;

    public FileReceived(int fileID, String fileName, byte[] fileData) {
        this.fileID = fileID;
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public int getFileID() {
        return fileID;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

}
