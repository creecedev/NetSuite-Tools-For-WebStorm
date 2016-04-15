package ui;

import com.intellij.openapi.project.Project;
import projectsettings.ProjectSettingsController;

import javax.swing.*;
import java.awt.event.*;

public class ProjectSettingsUI extends JDialog {

    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel nsAccountNameLabel;
    private JTextField nsAccountNameField;
    private JLabel nsAccountIdLabel;
    private JTextField nsAccountIdField;
    private JLabel nsEnvironmentLabel;
    private JTextField nsEnvironmentField;
    private JLabel nsAccountEmailLabel;
    private JTextField nsAccountEmailField;
    private JLabel nsAccountRoleLabel;
    private JTextField nsAccountRoleField;
    private JLabel nsRootFolderLabel;
    private JTextField nsRootFolderField;

    private JPanel contentPane;

    private Project project;

    public ProjectSettingsUI(Project project) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.project = project;

        setProjectSettingsUIFields();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void setProjectSettingsUIFields() {
        ProjectSettingsController projectSettingsController = new ProjectSettingsController(this.project);
        this.nsAccountNameField.setText(projectSettingsController.getNsAccountName());
        this.nsAccountIdField.setText(projectSettingsController.getNsAccount());
        this.nsEnvironmentField.setText(projectSettingsController.getNsEnvironment());
        this.nsAccountEmailField.setText(projectSettingsController.getNsEmail());
        this.nsAccountRoleField.setText(projectSettingsController.getNsAccountRole());
        this.nsRootFolderField.setText(projectSettingsController.getNsRootFolder());
    }
}
