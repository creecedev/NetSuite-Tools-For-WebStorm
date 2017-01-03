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
import java.math.RoundingMode;
import java.text.DecimalFormat;

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
                uploadFiles(project, projectHelper.getProjectRootDirectory(project), files, nsClient, projectSettingsController, progressIndicator);
            }
        });
    }

    private void uploadFiles(Project project, String projectBaseDirectory, VirtualFile[] files, NSClient nsClient, ProjectSettingsController projectSettingsController, ProgressIndicator progessIndicator) {
        if (files == null || files.length == 0) {
            return;
        }

        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                uploadFiles(project, projectBaseDirectory, file.getChildren(), nsClient, projectSettingsController, progessIndicator);
            } else {
                Document fileToSave = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
                    @Override
                    public Document compute() {
                        return getDocument(file);
                    }
                });

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                saveDocument(FileDocumentManager.getInstance(), fileToSave);
                            }
                        });
                    }
                }, ModalityState.NON_MODAL);

                final ProjectHelper projectHelper = new ProjectHelper();
                String projectFilePathFromRootDirectory = projectHelper.getProjectFilePathFromRootDirectory(file, projectBaseDirectory);

                if (projectFilePathFromRootDirectory != null) {

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
                        } else {
                            try {
                                String fileId = nsClient.searchFile(foldersAndFile[i], currentParentFolder, projectSettingsController.getNsRootFolder());
                                progessIndicator.setText("Uploading File: " + foldersAndFile[i]);
                                progessIndicator.setFraction(0);
                                WriteResponse response = nsClient.uploadFile(foldersAndFile[i], file.getPath(), fileId, currentParentFolder, "");

                                if (!response.getStatus().isIsSuccess()) {
                                    JBPopupFactory.getInstance()
                                            .createHtmlTextBalloonBuilder("<h3>" + foldersAndFile[i] + " Failed To Upload</h3>", MessageType.ERROR, null)
                                            .setFadeoutTime(3000)
                                            .createBalloon()
                                            .show(RelativePoint.getNorthEastOf(WindowManager.getInstance().getIdeFrame(project).getComponent()),
                                                    Balloon.Position.above);

                                    JOptionPane.showMessageDialog(null, "File: " + foldersAndFile[i] + "\n" +
                                                    "NetSuite File Cabinet Parent Folder ID: " + currentParentFolder + "\n" +
                                                    "Error Details: " + response.getStatus().getStatusDetail(response.getStatus().getStatusDetail().length - 1).getMessage(),
                                            "FILE UPLOAD ERROR",
                                            JOptionPane.ERROR_MESSAGE);
                                } else {
                                    progessIndicator.setFraction(1);
                                    JBPopupFactory.getInstance()
                                            .createHtmlTextBalloonBuilder("<h3>" + foldersAndFile[i] + " Uploaded Successfully</h3>", MessageType.INFO, null)
                                            .setFadeoutTime(3000)
                                            .createBalloon()
                                            .show(RelativePoint.getNorthEastOf(WindowManager.getInstance().getIdeFrame(project).getComponent()),
                                                    Balloon.Position.above);
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null, "Error uploading file", "ERROR", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        }
    }

    private Document getDocument(VirtualFile file) {
        return FileDocumentManager.getInstance().getDocument(file);
    }

    private void saveDocument(FileDocumentManager fileDocumentManager, Document document) {
        if (fileDocumentManager.isDocumentUnsaved(document)) {
            fileDocumentManager.saveDocument(document);
        }
    }

//    private void saveFile(VirtualFile file) {
//        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
//        Document document = fileDocumentManager.getDocument(file);
//
//        if (fileDocumentManager.isDocumentUnsaved(document)) {
//            fileDocumentManager.saveDocument(document);
//        }
//    }
}
