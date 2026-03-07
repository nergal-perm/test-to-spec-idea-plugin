package ewc.openspec.testlink.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(
    name = "TestToSpecSettings",
    storages = [Storage("testToSpec.xml")]
)
class TestToSpecSettings : PersistentStateComponent<TestToSpecSettings.State> {

    class State {
        var specRootPath: String = "specs"
        var scenarioAnnotationFqn: String = ""
        var capabilityAttribute: String = "capability"
        var valueAttribute: String = "value"
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): TestToSpecSettings =
            project.getService(TestToSpecSettings::class.java)
    }
}
