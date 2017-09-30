package ui;

import com.netsuite.webservices.documents.filecabinet_2017_1.Folder;

public class FileTreeFolder {

    private Folder folder;

    public FileTreeFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    public String toString() {
        return folder.getName();
    }
}
