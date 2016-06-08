package actions;

import com.intellij.diff.*;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.netsuite.webservices.documents.filecabinet_2016_1.File;
import netsuite.NSAccount;
import netsuite.NSClient;
import projectsettings.ProjectSettingsController;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CompareWithFileCabinetAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        ProjectSettingsController projectSettingsController = new ProjectSettingsController(project);
        e.getPresentation().setVisible(file != null && !file.isDirectory() && projectSettingsController.hasAllProjectSettings());
        e.getPresentation().setEnabled(file != null && !file.isDirectory() && projectSettingsController.hasAllProjectSettings());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile[] projectFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        ProjectHelper projectHelper = new ProjectHelper();
        ProjectSettingsController projectSettingsController = new ProjectSettingsController(e.getProject());
        String currentEnvironment = projectSettingsController.getNsEnvironment();
        NSAccount nsAccount = projectSettingsController.getNSAccountForProject();

        if (nsAccount == null) {
            return;
        }

        NSClient nsClient = null;

        try {
            nsClient = new NSClient(nsAccount, currentEnvironment);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Exception creating new NSClient" + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<String> fileIds = getSelectedFileIds(projectHelper.getProjectRootDirectory(project), projectFiles, nsClient, projectSettingsController);

        if (fileIds == null || fileIds.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Selected file(s) do not exist in file cabinet for this path", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<File> files = new ArrayList<>();

        for (String fileId : fileIds) {
            try {
                files.add(nsClient.downloadFile(fileId));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Exception downloading file from NetSuite File Cabinet: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (files.isEmpty() || files.size() != projectFiles.length) {
            return;
        }

        int selectedFileNum = 0;
        for (File file : files) {
            if (file != null) {
                final DiffContent remoteFile = DiffContentFactory.getInstance().create(new String(file.getContent(), StandardCharsets.UTF_8));

                final DiffContent localFile = DiffContentFactory.getInstance().create(project, projectFiles[selectedFileNum]);

                DiffRequest dr = new SimpleDiffRequest("NetSuite File Cabinet Compare", remoteFile, localFile, "NetSuite File Cabinet", "Local File");
                DiffManager.getInstance().showDiff(e.getProject(), dr);
            }

            selectedFileNum++;
        }
    }

    private ArrayList<String> getSelectedFileIds(String projectBaseDirectory, VirtualFile[] files, NSClient nsClient, ProjectSettingsController projectSettingsController) {
        if (files == null || files.length == 0) {
            return null;
        }

        ArrayList<String> fileIds = new ArrayList<String>();
        final ProjectHelper projectHelper = new ProjectHelper();

        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                getSelectedFileIds(projectBaseDirectory, file.getChildren(), nsClient, projectSettingsController);
            } else {
                String projectFilePathFromRootDirectory = projectHelper.getProjectFilePathFromRootDirectory(file, projectBaseDirectory);

                if (projectFilePathFromRootDirectory == null) {
                    return null;
                }

                String[] foldersAndFile = projectFilePathFromRootDirectory.split("/");
                String currentParentFolder =  projectSettingsController.getNsRootFolder();

                for (int i = 0; i < foldersAndFile.length; i++) {
                    if ((i + 1) != foldersAndFile.length) {
                        try {
                            String folderId = nsClient.searchFolder(foldersAndFile[i], currentParentFolder);

                            if (folderId == null) {
                                return null;
                            }

                            currentParentFolder = folderId;
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Exception searching for Folder in NetSuite File Cabinet: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    } else {
                        try {
                            if (currentParentFolder != null) {
                                String fileId = nsClient.searchFile(foldersAndFile[i], currentParentFolder, projectSettingsController.getNsRootFolder());

                                if (fileId != null) {
                                    fileIds.add(nsClient.searchFile(foldersAndFile[i], currentParentFolder, projectSettingsController.getNsRootFolder()));
                                } else {
                                    JOptionPane.showMessageDialog(null, "File: " + foldersAndFile[i] + " Does not exist in NetSuite File Cabinet", "ERROR", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Exception searching for File in NetSuite File Cabinet: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }
                }
            }
        }

        return fileIds;
    }
}
