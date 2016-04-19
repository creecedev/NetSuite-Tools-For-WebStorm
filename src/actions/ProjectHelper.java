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
        String[] filePath = file.getPath().split("/");
        String path = "";
        Boolean foundRoot = false;

        for (String pathPiece : filePath) {
            if (foundRoot) {
                if (!path.isEmpty()) {
                    path = path.concat("/");
                }

                path = path.concat(pathPiece);
            }

            if (pathPiece.equals(projectBaseDirectory)) {
                foundRoot = true;
            }
        }

        if (path.isEmpty()) {
            return null;
        }

        return path;
    }
}
