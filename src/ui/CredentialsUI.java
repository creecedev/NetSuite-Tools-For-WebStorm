package ui;

import com.intellij.ide.BrowserUtil;
import projectsettings.ProjectSettingsController;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.event.*;

public class CredentialsUI extends JDialog {
    private JPanel contentPane;
    private JButton nextButton;
    private JButton cancelButton;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel emailLabel;
    private JLabel passwordLabel;
    private JComboBox environmentComboBox;
    private JLabel environmentLabel;
    private Project project;

    public CredentialsUI(Project project) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(nextButton);

        this.project = project;

        ProjectSettingsController projectSettingsController = new ProjectSettingsController(this.project);

        if (projectSettingsController.hasAllProjectSettings()) {
            String nsPassword = projectSettingsController.getProjectPassword();
            if (nsPassword != null && !nsPassword.isEmpty()) {
                emailField.setText(projectSettingsController.getNsEmail());
                passwordField.setText(nsPassword);
                environmentComboBox.setSelectedItem(projectSettingsController.getNsEnvironment());
            }
        }

        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNext();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
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

    private void onNext() {
        if (emailField.getText().isEmpty() || String.valueOf(passwordField.getPassword()).isEmpty() || environmentComboBox.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(null, "Email, Password and Environment are required",  "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        this.setVisible(false);
        AccountsUI accountsUI = new AccountsUI(emailField.getText(), String.valueOf(passwordField.getPassword()), environmentComboBox.getSelectedItem().toString(), this.project);

        if (accountsUI.getNsAccounts() != null) {
            accountsUI.pack();
            accountsUI.setLocationRelativeTo(null);
            accountsUI.setVisible(true);
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
