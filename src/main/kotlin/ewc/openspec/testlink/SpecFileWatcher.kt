package ewc.openspec.testlink

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.VirtualFileManager
import ewc.openspec.testlink.settings.TestToSpecSettings

class SpecFileWatcher : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.messageBus.connect(project).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val basePath = project.basePath ?: return
                    val specRoot = "$basePath/${TestToSpecSettings.getInstance(project).state.specRootPath}"
                    if (events.any { it.path.startsWith(specRoot) && it.path.endsWith(".md") }) {
                        DaemonCodeAnalyzer.getInstance(project).restart()
                    }
                }
            }
        )
    }
}
