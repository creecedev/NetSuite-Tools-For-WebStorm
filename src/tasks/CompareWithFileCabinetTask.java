package tasks;

import actions.ProjectHelper;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.netsuite.webservices.documents.filecabinet_2016_2.File;
import netsuite.NSClient;
import projectsettings.ProjectSettingsController;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class CompareWithFileCabinetTask implements Runnable {
    private Project project;
    private VirtualFile[] projectFiles;
    private ProjectHelper projectHelper = new ProjectHelper();
    private NSClient nsClient;
    private ProjectSettingsController projectSettingsController;
    private DecimalFormat decimalFormat = new DecimalFormat("##.##");

    public CompareWithFileCabinetTask(Project project, VirtualFile[] files, NSClient nsClient, ProjectSettingsController projectSettingsController) {
        this.project = project;
        this.projectFiles = files;
        this.nsClient = nsClient;
        this.projectSettingsController = projectSettingsController;
    }

    @Override
    public void run() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Preparing to compare selected file(s) to NetSuite File Cabinet") {
            public void run(final ProgressIndicator progressIndicator) {
                try {
                    ArrayList<String> fileIds = getSelectedFileIds(projectHelper.getProjectRootDirectory(project), projectFiles, nsClient, projectSettingsController);

                    if (fileIds == null || fileIds.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Selected file(s) do not exist in file cabinet for this path", "ERROR", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ArrayList<File> files = downloadFiles(fileIds, progressIndicator);

                    if (files == null || files.isEmpty() || files.size() != projectFiles.length) {
                        return;
                    }

                    progressIndicator.setFraction(0);
                    progressIndicator.setText("Showing diffs");
                    for (File file : files) {
                        showDiffForFiles(file, getLocalFile(file.getName()));
                    }
                    progressIndicator.setFraction(1);
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(null, "Exception: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private ArrayList<String> getSelectedFileIds(String projectBaseDirectory, VirtualFile[] files, NSClient nsClient, ProjectSettingsController projectSettingsController) {
        if (files == null || files.length == 0) {
            return null;
        }

        ArrayList<String> fileIds = new ArrayList<String>();

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
                                    fileIds.add(fileId);
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

    private ArrayList<File> downloadFiles(ArrayList<String> fileIds, ProgressIndicator progressIndicator) {
        ArrayList<File> files = new ArrayList<File>();
        progressIndicator.setText("Downloading selected file(s) from NetSuite File Cabinet");

        double numberOfFilesToDownload = fileIds.size();
        double currentFileNumber = 1;

        for (String fileId : fileIds) {
            progressIndicator.setFraction(currentFileNumber/numberOfFilesToDownload);
            progressIndicator.setText("Downloading selected file(s) from NetSuite File Cabinet: " + decimalFormat.format((currentFileNumber/numberOfFilesToDownload) * 100) + "% Complete");
            try {
                File downloadedFile = nsClient.downloadFile(fileId);

                if (downloadedFile != null) {
                    files.add(nsClient.downloadFile(fileId));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Exception downloading file from NetSuite File Cabinet: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
            currentFileNumber++;
        }

        progressIndicator.setFraction(1);
        return files;
    }

    private VirtualFile getLocalFile(String remoteFileName) {
        for (VirtualFile file : projectFiles) {
            if (remoteFileName.equals(file.getName())) {
                return file;
            }
        }

        return null;
    }

    private void showDiffForFiles(File remoteFile, VirtualFile localFile) {
        if (remoteFile == null || localFile == null) {
            return;
        }

        final DiffContent remoteFileContent = DiffContentFactory.getInstance().create(new String(remoteFile.getContent(), StandardCharsets.UTF_8));

        final DiffContent localFileContent = DiffContentFactory.getInstance().create(project, localFile);

        DiffRequest dr = new SimpleDiffRequest("NetSuite File Cabinet Compare", remoteFileContent, localFileContent, "NetSuite File Cabinet - " + remoteFile.getName(), "Local File - " + localFile.getName());

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        DiffManager.getInstance().showDiff(project, dr);
                    }
                });
            }
        }, ModalityState.NON_MODAL);
    }
}