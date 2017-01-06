package tasks;

import actions.ProjectHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.netsuite.webservices.platform.messages_2016_2.WriteResponse;
import netsuite.NSClient;
import projectsettings.ProjectSettingsController;

import javax.swing.*;

public class UploadTask implements Runnable {

    private Project project;
    private VirtualFile[] files;
    private ProjectHelper projectHelper = new ProjectHelper();
    private NSClient nsClient;
    private ProjectSettingsController projectSettingsController;

    public UploadTask(Project project, VirtualFile[] files, NSClient nsClient, ProjectSettingsController projectSettingsController) {
        this.project = project;
        this.files = files;
        this.nsClient = nsClient;
        this.projectSettingsController = projectSettingsController;
    }

    @Override
    public void run() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Preparing to upload selected file(s) to NetSuite File Cabinet") {
            public void run(final ProgressIndicator progressIndicator) {
                uploadFiles(null, progressIndicator);
            }
        });
    }

    private void uploadFiles(VirtualFile[] fileChildren, ProgressIndicator progessIndicator) {
        VirtualFile[] files = this.files;

        if (fileChildren != null) {
            files = fileChildren;
        }

        if (files == null || files.length == 0) {
            return;
        }

        String projectRootDirectory = projectHelper.getProjectRootDirectory(project);

        /*
         * If uploading a directory of files, then recursively call uploadFiles with the children
         * files of the directory to upload them. Otherwise, for each file, attempt to get the NetSuite
         * parent folder ID of the file being uploaded (folders are created if they do not exist)
         * and upload the file into its parent directory.
         */
        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                uploadFiles(file.getChildren(), progessIndicator);
            } else {
                saveDocument(file);
                String fileNetSuiteParentFolderId = getFileNetSuiteParentFolderId(file, projectRootDirectory);

                if (fileNetSuiteParentFolderId != null)
                {
                    progessIndicator.setFraction(0);
                    progessIndicator.setText("Uploading File: " + file.getName());
                    try {
                        WriteResponse response = nsClient.uploadFile(file.getName(), file.getPath(), nsClient.searchFile(file.getName(), fileNetSuiteParentFolderId, projectSettingsController.getNsRootFolder()), fileNetSuiteParentFolderId, "");
                        if (!response.getStatus().isIsSuccess()) {
                            displayUploadResultBalloonMessage(file.getName(), false);
                            JOptionPane.showMessageDialog(null, "File: " + file.getName() + "\n" +
                                            "NetSuite File Cabinet Parent Folder ID: " + fileNetSuiteParentFolderId + "\n" +
                                            "Error Details: " + response.getStatus().getStatusDetail(response.getStatus().getStatusDetail().length - 1).getMessage(),
                                    "FILE UPLOAD ERROR",
                                    JOptionPane.ERROR_MESSAGE);
                        } else {
                            progessIndicator.setFraction(1);
                            displayUploadResultBalloonMessage(file.getName(), true);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Error uploading file", "ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private String getFileNetSuiteParentFolderId(VirtualFile file, String projectRootDirectory) {
        String projectFilePathFromRootDirectory = projectHelper.getProjectFilePathFromRootDirectory(file, projectRootDirectory);

        if (projectFilePathFromRootDirectory == null) {
            return null;
        }

        String[] foldersAndFile = projectFilePathFromRootDirectory.split("/");
        String currentParentFolder =  projectSettingsController.getNsRootFolder();

        for (int i = 0; i < foldersAndFile.length; i++) {
            if (i + 1 != foldersAndFile.length) {
                try {
                    String folderId = nsClient.searchFolder(foldersAndFile[i], currentParentFolder);

                    if (folderId == null) {
                        folderId = nsClient.createFolder(foldersAndFile[i], currentParentFolder);
                    }

                    currentParentFolder = folderId;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error Searching/Creating Folder", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        return currentParentFolder;
    }

    private void displayUploadResultBalloonMessage(String fileName, Boolean isSuccess) {
        String message = fileName + " Uploaded Successfully";
        MessageType messageType = MessageType.INFO;

        if (!isSuccess) {
            message = fileName + " Failed to Upload";
            messageType = MessageType.ERROR;
        }

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("<h3>" + message + "</h3>", messageType, null)
                .setFadeoutTime(3000)
                .createBalloon()
                .show(RelativePoint.getNorthEastOf(WindowManager.getInstance().getIdeFrame(project).getComponent()),
                        Balloon.Position.above);
    }

    private void saveDocument(VirtualFile file) {
        if (file == null) {
            return;
        }

        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();

        Document documentToSave = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
            @Override
            public Document compute() {
                return fileDocumentManager.getDocument(file);
            }
        });

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        if (documentToSave != null && fileDocumentManager.isDocumentUnsaved(documentToSave)) {
                            fileDocumentManager.saveDocument(documentToSave);
                        }
                    }
                });
            }
        }, ModalityState.NON_MODAL);
    }
}
