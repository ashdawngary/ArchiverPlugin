package utils;

import com.intellij.openapi.progress.ProgressIndicator;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipperUtils {

  /**
   * Compresses a specific dir from the target folder
   *
   * @param zf           zip buffer
   * @param localRoot    root to start compressing from
   * @param trueRootPath true root of workspace (to compute relative paths)
   */
  public static void compressToAccumulate(
      ZipOutputStream zf, String localRoot, Path trueRootPath,
      final Optional<ProgressIndicator> pi) {
    final Path localRootPath = Paths.get(localRoot);
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
