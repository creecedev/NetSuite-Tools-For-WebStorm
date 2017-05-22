package actions;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.vcs.changes.RunnableBackgroundableWrapper;

import com.netsuite.webservices.documents.filecabinet_2016_2.File;
import com.netsuite.webservices.documents.filecabinet_2016_2.Folder;
import com.netsuite.webservices.platform.core_2016_2.SearchResult;
import projectsettings.ProjectSettingsController;
import netsuite.*;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.io.*;

public class DownloadProjectAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
//        final Project project = e.getProject();
//        ProjectSettingsController projectSettingsController = new ProjectSettingsController(project);
//        e.getPresentation().setVisible(projectSettingsController.hasAllProjectSettings());
//        e.getPresentation().setEnabled(projectSettingsController.hasAllProjectSettings());
    }

    public static void copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
        byte[] buf = new byte[bufferSize];
        int n = input.read(buf);
        while (n >= 0) {
            output.write(buf, 0, n);
            n = input.read(buf);
        }
        output.flush();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();



//        final VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

        ProjectSettingsController projectSettingsController = new ProjectSettingsController(project);

        String currentEnvironment = projectSettingsController.getNsEnvironment();

        NSAccount nsAccount = projectSettingsController.getNSAccountForProject();

        if (nsAccount == null) {
            return;
        }

        final NSClient nsClient;

        try {
            nsClient = new NSClient(nsAccount, currentEnvironment);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error creating NSClient", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SearchResult rootDirectoryFolders = null;

        try {
            rootDirectoryFolders = nsClient.getSubFolders(projectSettingsController.getNsRootFolder());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error getting Sub Folders",  "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int i = 0; i < rootDirectoryFolders.getTotalRecords(); i++) {
            Folder baseDirectoryFolder = (Folder)rootDirectoryFolders.getRecordList().getRecord(i);

            System.out.println("Folder Name: " + baseDirectoryFolder.getName() + " | Folder ID: " + baseDirectoryFolder.getInternalId());

            try {
                project.getBaseDir().createChildDirectory(this, baseDirectoryFolder.getName());
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }



            SearchResult filesInFolder = null;

            try {
                System.out.println("Retrieving files with parent folder ID: " + baseDirectoryFolder.getInternalId());
                filesInFolder = nsClient.getFilesForFolder(baseDirectoryFolder.getInternalId());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error getting files for folder",  "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (filesInFolder != null && filesInFolder.getTotalRecords() > 0) {

                System.out.println("================ FILE LIST START ================");
                for (int j = 0; j < filesInFolder.getTotalRecords(); j++) {
                    File foundFile = (File)filesInFolder.getRecordList().getRecord(j);
                    if (foundFile.getFolder().getInternalId().equals(baseDirectoryFolder.getInternalId())) {
                        System.out.println("File Name: " + foundFile.getName() + " File ID: " + foundFile.getInternalId() + " Parent Folder ID: " + foundFile.getFolder().getInternalId());
                    }
                }
                System.out.println("================ FILE LIST END ================");
                System.out.println("\n");
            } else {
                System.out.println("No Files in Folder with ID: " + baseDirectoryFolder.getInternalId());
                System.out.println("\n");
            }
        }

        // for each folder in a folder list
        // create folder
        // if that folder has any sub folders
        // back to top
        // else download files

        System.out.println("PAUSE");
//        RunnableBackgroundableWrapper wrapper = new RunnableBackgroundableWrapper(e.getProject(), "", new UploadTask(project, files, nsClient, projectSettingsController));
//        ProgressWindow progressIndicator = new ProgressWindow(true, project);
//        progressIndicator.setIndeterminate(true);
//
//        ProgressManager.getInstance().runProcessWithProgressAsynchronously(wrapper, progressIndicator);
    }
}
