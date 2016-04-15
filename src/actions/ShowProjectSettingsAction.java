package actions;

import com.intellij.openapi.project.Project;
import projectsettings.ProjectSettingsController;
import ui.ProjectSettingsUI;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ShowProjectSettingsAction extends AnAction {

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
        ProjectSettingsUI projectSettingsUI = new ProjectSettingsUI(project);
        projectSettingsUI.pack();
        projectSettingsUI.setLocationRelativeTo(null);
        projectSettingsUI.setVisible(true);
    }
}
