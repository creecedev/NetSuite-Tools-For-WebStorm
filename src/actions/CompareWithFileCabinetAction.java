package actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.RunnableBackgroundableWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import netsuite.NSAccount;
import netsuite.NSClient;
import projectsettings.ProjectSettingsController;
import tasks.CompareWithFileCabinetTask;

import javax.swing.*;

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
        final VirtualFile[] files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);

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

        RunnableBackgroundableWrapper wrapper = new RunnableBackgroundableWrapper(e.getProject(), "", new CompareWithFileCabinetTask(project, files, nsClient, projectSettingsController));
        ProgressWindow progressIndicator = new ProgressWindow(true, project);
        progressIndicator.setIndeterminate(true);

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(wrapper, progressIndicator);
    }
}
