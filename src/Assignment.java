import com.intellij.ide.UiActivity.Progress;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * lmao imagine using javadoc
 */
public class Assignment {

  private final @NotNull
  Project proj;
  private final @NotNull
  VirtualFile src;
  private final @NotNull
  VirtualFile test;
  private final @NotNull
  VirtualFile root;

  private Optional<ProgressIndicator> indicate;

  public Assignment(@NotNull VirtualFile root, @NotNull Project proj) {
    this.proj = Objects.requireNonNull(proj, "missing project file");
    this.root = Objects.requireNonNull(root, "missing root file");
    this.src = Objects
        .requireNonNull(root.findFileByRelativePath("./test"), "missing testing directory");
    this.test = Objects
        .requireNonNull(root.findFileByRelativePath("./src"), "missing sources directory");
  }

  public String toZip() throws IOException {
    return toZip(null);
  }

  /**
   * Compresses Assignment to Zip
   *
   * @param providedIndicator progress bar for notifs.
   * @return string path to zip
   * @throws IOException if zipbuffer crashes :(
   */
  public String toZip(@Nullable ProgressIndicator providedIndicator) throws IOException {
    this.indicate = Optional.ofNullable(providedIndicator);

    final Path rootPath = Paths.get(Objects.requireNonNull(this.root.getPath()));
    final Path projectPath = Paths.get(Objects.requireNonNull(this.proj.getBasePath()));
    final String zipFileName = Paths.get(projectPath.toString(), this.proj.getName() + ".zip")
        .toString();

    this.indicate.ifPresent(indicator -> indicator.setText("Making " + this.proj.getName() + ".zip"));

    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFileName));
    compressToAccumulate(zout, this.src.getPath(), rootPath);
    compressToAccumulate(zout, this.test.getPath(), rootPath);
    zout.close();

    this.indicate.ifPresent(indicator -> indicator.setText2(""));
    this.indicate.ifPresent(indicator -> indicator.setText("Done!"));


    return zipFileName;
  }

  /**
   * Compresses a specific dir from the target folder
   *
   * @param zf           zip buffer
   * @param localRoot    root to start compressing from
   * @param trueRootPath true root of workspace (to compute relative paths)
   */
  private void compressToAccumulate(ZipOutputStream zf, String localRoot, Path trueRootPath) {
    final Path localRootPath = Paths.get(localRoot);
    final Optional<ProgressIndicator> pi = this.indicate;
    try {
      Files.walkFileTree(localRootPath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes ignored) {
          try {
            Path targetFile = trueRootPath.relativize(file);
            pi.ifPresent(indicator -> indicator.setText2(targetFile.toString()));
            zf.putNextEntry(new ZipEntry(targetFile.toString()));
            byte[] bytes = Files.readAllBytes(file);
            zf.write(bytes, 0, bytes.length);
            zf.closeEntry();
          } catch (IOException e) {
            e.printStackTrace();
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
