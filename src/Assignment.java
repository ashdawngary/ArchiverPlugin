import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Objects;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

/**
 * lmao imagine using javadoc
 */
public class Assignment {

  private final Project proj;
  private VirtualFile src;
  private VirtualFile test;
  private final VirtualFile root;

  public Assignment(@NotNull VirtualFile root, @NotNull Project proj) {
    this.proj = Objects.requireNonNull(proj, "project file");
    this.root = Objects.requireNonNull(root, "root file");
    VirtualFile testDir = Objects
        .requireNonNull(root.findFileByRelativePath("./test"), "testing directory");
    VirtualFile srcDir = Objects
        .requireNonNull(root.findFileByRelativePath("./src"), "sources directory");
  }


}
