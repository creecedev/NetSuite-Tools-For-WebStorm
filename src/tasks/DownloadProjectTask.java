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
import com.netsuite.webservices.documents.filecabinet_2016_2.File;
import com.netsuite.webservices.platform.messages_2016_2.WriteResponse;
import netsuite.NSClient;
import projectsettings.ProjectSettingsController;

import javax.swing.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class DownloadProjectTask implements Runnable {

    private Project project;
    private ProjectHelper projectHelper = new ProjectHelper();
    private NSClient nsClient;
    private ProjectSettingsController projectSettingsController;
    private DecimalFormat decimalFormat = new DecimalFormat("##.##");

    public DownloadProjectTask(Project project, NSClient nsClient, ProjectSettingsController projectSettingsController) {
        this.project = project;
        this.nsClient = nsClient;
        this.projectSettingsController = projectSettingsController;
    }

    @Override
    public void run() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Preparing to upload selected file(s) to NetSuite File Cabinet") {
            public void run(final ProgressIndicator progressIndicator) {
                downloadProject(null, progressIndicator);
            }
        });
    }

    private ArrayList<File> downloadProject(ArrayList<String> fileIds, ProgressIndicator progressIndicator) {
//        ArrayList<File> files = new ArrayList<File>();
//        progressIndicator.setText("Downloading selected file(s) from NetSuite File Cabinet");
//
//        double numberOfFilesToDownload = fileIds.size();
//        double currentFileNumber = 1;
//
//        for (String fileId : fileIds) {
//            progressIndicator.setFraction(currentFileNumber/numberOfFilesToDownload);
//            progressIndicator.setText("Downloading selected file(s) from NetSuite File Cabinet: " + decimalFormat.format((currentFileNumber/numberOfFilesToDownload) * 100) + "% Complete");
//            try {
//                File downloadedFile = nsClient.downloadFile(fileId);
//
//                if (downloadedFile != null) {
//                    files.add(nsClient.downloadFile(fileId));
//                }
//            } catch (Exception ex) {
//                JOptionPane.showMessageDialog(null, "Exception downloading file from NetSuite File Cabinet: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
//            }
//            currentFileNumber++;
//        }
//
//        progressIndicator.setFraction(1);
//        return files;

        return null;
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
