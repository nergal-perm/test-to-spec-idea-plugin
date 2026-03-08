package ewc.openspec.testlink

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object Icons {
    @JvmField
    val SCENARIO: Icon = IconLoader.getIcon("/icons/scenario.svg", Icons::class.java)

    @JvmField
    val SCENARIO_UNLINKED: Icon = IconLoader.getIcon("/icons/scenario_unlinked.svg", Icons::class.java)
}
