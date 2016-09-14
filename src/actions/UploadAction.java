package actions;

import com.netsuite.webservices.platform.messages_2016_1.WriteResponse;

import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import projectsettings.ProjectSettingsController;
import netsuite.*;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

public class UploadAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getProject();
        ProjectSettingsController projectSettingsController = new ProjectSettingsController(project);
        e.getPresentation().setVisible(projectSettingsController.hasAllProjectSettings());
        e.getPresentation().setEnabled(projectSettingsController.hasAllProjectSettings());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        final ProjectHelper projectHelper = new ProjectHelper();

        ProjectSettingsController projectSettingsController = new ProjectSettingsController(project);

        String currentEnvironment = projectSettingsController.getNsEnvironment();

        NSAccount nsAccount = projectSettingsController.getNSAccountForProject();

        if (nsAccount == null) {
            return;
        }

        NSClient nsClient = null;

        try {
            nsClient = new NSClient(nsAccount, currentEnvironment);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error creating NSClient", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        processFiles(project, projectHelper.getProjectRootDirectory(project), files, nsClient, projectSettingsController);
    }

    private void processFiles(Project project, String projectBaseDirectory, VirtualFile[] files, NSClient nsClient, ProjectSettingsController projectSettingsController) {
        if (files == null || files.length == 0) {
            return;
        }

        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                processFiles(project, projectBaseDirectory, file.getChildren(), nsClient, projectSettingsController);
            } else {
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
}
