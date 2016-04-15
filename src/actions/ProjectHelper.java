package actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class ProjectHelper {

    public String getProjectRootDirectory(Project project) {
        String[] projectFilePathPieces = project.getBasePath().split("/");

        if (projectFilePathPieces != null && projectFilePathPieces.length > 0) {
            return projectFilePathPieces[projectFilePathPieces.length - 1];
        }

        return null;
    }

    public String getProjectFilePathFromRootDirectory(VirtualFile file , String projectBaseDirectory) {
        String[] filePath = file.getPath().split(projectBaseDirectory);

        if (filePath.length > 0) {
            String path = filePath[filePath.length-1];
            return path.substring(1, path.length());
        }

        return null;
    }
}
