package actions;

import com.intellij.openapi.project.Project;
import projectsettings.ProjectSettingsController;
import ui.CredentialsUI;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ChangeProjectSettingsAction extends AnAction {

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
        CredentialsUI credentialsUI = new CredentialsUI(project);
        credentialsUI.pack();
        credentialsUI.setLocationRelativeTo(null);
        credentialsUI.setVisible(true);
    }
}
