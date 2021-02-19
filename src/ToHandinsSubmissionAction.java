import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;


public class ToHandinsSubmissionAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    // Using the event, evaluate the context, and enable or disable the action.
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {

    Project currentProject = Objects.requireNonNull(event.getProject());
    VirtualFile[] toplevel = ProjectRootManager.getInstance(currentProject).getContentRoots();

    if (toplevel.length > 1) {
      // too many files, not a valid project.
      return;
    }

    try {
      ProgressManager
          .getInstance().run(
          new Modal(currentProject, "Making archive for " + currentProject.getName(),
              false) {

            public void announce(String title, String message){
              NotificationGroup ng = new NotificationGroup("demo.notifications.toolWindow",
                  NotificationDisplayType.TOOL_WINDOW,
                  true);
              final Notification finished = ng.createNotification("", title,
                  message,
                  NotificationType.INFORMATION);

              finished.notify(currentProject);

            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              indicator.setIndeterminate(true);
              indicator.setText("Checking pre-reqs");

              Assignment handinsSubmit;
              try {
                handinsSubmit = new Assignment(toplevel[0], currentProject);
              } catch (NullPointerException nx) {
                announce("Could not Make Archive", nx.getMessage());
                return;
              }

              String archiveLoc = null;
              try {
                archiveLoc = handinsSubmit.toZip(indicator);
              } catch (IOException e) {
                announce("Failed to Compress Archive", e.getMessage());
                return;
              }

              Toolkit.getDefaultToolkit()
                  .getSystemClipboard()
                  .setContents(
                      new StringSelection(archiveLoc),
                      null
                  );
              announce("Compressed", "Successfully created: " + archiveLoc);
            }
          });


    } catch (Exception e) {
      System.out.println("it crashed because of: " + e.getMessage());
    }

  }

}
