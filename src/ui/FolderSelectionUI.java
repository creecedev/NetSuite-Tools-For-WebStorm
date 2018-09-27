package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.netsuite.webservices.documents.filecabinet_2017_1.Folder;
import com.netsuite.webservices.platform.core_2017_1.SearchResult;

import netsuite.NSClient;
import projectsettings.ProjectSettingsController;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;

public class FolderSelectionUI extends JDialog {
    private JPanel     contentPane;
    private JButton    cancelButton;
    private JButton    selectAndCloseButton;
    private JTree      nsFolderTree;
    private JTextField rootFolderIdTextField;
    private JLabel     specifyRootFolderInternalIdLabel;
    private NSClient   nsClient;
    private Project    project;
    private String     nsEnvironment;

    final private String SUITESCRIPTS_FOLDER_INTERNAL_ID = "-15";
    final private String SUITEBUNDLES_FOLDER_INTERNAL_ID = "-16";
    final private String FILE_CABINET_ROOT_INTERNAL_ID   = "";
    final private String FILE_CABINET_ROOT               = "File Cabinet";
    final private String FILE_CABINET_SUITESCRIPTS       = "SuiteScripts";
    final private String FILE_CABINET_SUITEBUNDLES       = "SuiteBundles";

    public FolderSelectionUI(NSClient nsClient, Project project, String selectedEnvironment) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(selectAndCloseButton);

        this.nsClient      = nsClient;
        this.project       = project;
        this.nsEnvironment = selectedEnvironment;

        initializeFolderSelectionUI();

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        selectAndCloseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
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

    private void initializeFolderSelectionUI() {
        try {
            Folder rootFolder = new Folder();
            rootFolder.setName(FILE_CABINET_ROOT);

            DefaultTreeModel model = (DefaultTreeModel) nsFolderTree.getModel();
            model.setRoot(new DefaultMutableTreeNode(new FileTreeFolder(rootFolder)));

            addSubdirectoriesToFolderTree(model, SUITESCRIPTS_FOLDER_INTERNAL_ID, FILE_CABINET_SUITESCRIPTS);
            addSubdirectoriesToFolderTree(model, SUITEBUNDLES_FOLDER_INTERNAL_ID, FILE_CABINET_SUITEBUNDLES);

            nsFolderTree.setModel(model);
            nsFolderTree.setCellRenderer(new NSFileTreeRenderer());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error initializing Folder Selection UI",  "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addSubdirectoriesToFolderTree(DefaultTreeModel model, String parentFolderId, String parentFolderName) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();

        Folder subDirectoryRoot = new Folder();
        subDirectoryRoot.setName(parentFolderName);
        subDirectoryRoot.setInternalId(parentFolderId);

        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(new FileTreeFolder(subDirectoryRoot));

        model.insertNodeInto(folderNode, root, root.getChildCount());

        SearchResult folderResults = null;

        try {
            if (parentFolderId == FILE_CABINET_ROOT_INTERNAL_ID){
                parentFolderId = null;
            }
            folderResults = this.nsClient.getSubFolders(parentFolderId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error getting Sub Folders",  "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (folderResults == null) {
            return;
        }

        for (int i = 0; i < folderResults.getTotalRecords(); i++) {
            Folder foundFolder = (Folder)folderResults.getRecordList().getRecord(i);
            model.insertNodeInto(new DefaultMutableTreeNode(new FileTreeFolder(foundFolder)), folderNode, folderNode.getChildCount());
        }
    }

    private void saveProjectSettings(String nsRootFolderId) {
        ProjectSettingsController nsProjectSettingsController = new ProjectSettingsController(this.project);
        nsProjectSettingsController.setNsEmail(this.nsClient.getNSAccount().getAccountEmail());
        nsProjectSettingsController.setNsRootFolder(nsRootFolderId);
        nsProjectSettingsController.setNsAccount(this.nsClient.getNSAccount().getAccountId());
        nsProjectSettingsController.setNsAccountName(this.nsClient.getNSAccount().getAccountName());
        nsProjectSettingsController.setNsAccountRole(this.nsClient.getNSAccount().getRoleId());
        nsProjectSettingsController.setNsEnvironment(this.nsEnvironment);
        nsProjectSettingsController.saveProjectPassword(this.nsClient);
    }

    private void onOK() {
        String nsRootFolderId = null;

        TreePath selectedFilePath = nsFolderTree.getSelectionPath();

        if (selectedFilePath!= null && rootFolderIdTextField.getText().isEmpty()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)nsFolderTree.getSelectionPath().getLastPathComponent();
            FileTreeFolder folder = (FileTreeFolder)node.getUserObject();
            String folderName = folder.getFolder().getName();

            if (folderName == null) {
                JOptionPane.showMessageDialog(null, "Either a Folder or Folder ID must be specified",  "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (folderName.equals(FILE_CABINET_ROOT)) {
                int confirmed = JOptionPane.showConfirmDialog(null, "You are attempting to set this project's root directory to " + folderName + ". This is the root folder of the NetSuite File Cabinet. \nThis is not recommended. Are you sure?",  "WARNING", JOptionPane.CANCEL_OPTION);
                if (confirmed != 0) {
                    return;
                }
                nsRootFolderId = FILE_CABINET_ROOT_INTERNAL_ID;
            } else {
                nsRootFolderId = folder.getFolder().getInternalId();
            }
        } else {
            nsRootFolderId = rootFolderIdTextField.getText().trim();
        }

        if (nsRootFolderId == null) {
            JOptionPane.showMessageDialog(null, "Folder must be selected or Folder ID specified",  "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveProjectSettings(nsRootFolderId);
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("<h3>NetSuite Project Settings Updated!</h3>", MessageType.INFO, null)
                .setFadeoutTime(3000)
                .createBalloon()
                .show(RelativePoint.getNorthEastOf(WindowManager.getInstance().getIdeFrame(project).getComponent()),
                        Balloon.Position.above);

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private static class NSFileTreeRenderer extends DefaultTreeCellRenderer {

        final private String FILE_VIEW_DIRECTORY_ICON = "FileView.directoryIcon";

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                setIcon(UIManager.getIcon(FILE_VIEW_DIRECTORY_ICON));
            }

            return this;
        }

    }
}
