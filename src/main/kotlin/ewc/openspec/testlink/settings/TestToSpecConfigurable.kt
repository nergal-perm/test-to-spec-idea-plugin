package ewc.openspec.testlink.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class TestToSpecConfigurable(private val project: Project) : Configurable {

    private var specRootField: JBTextField? = null
    private var annotationFqnField: JBTextField? = null
    private var capabilityAttrField: JBTextField? = null
    private var valueAttrField: JBTextField? = null

    override fun getDisplayName(): String = "Test to Spec"

    override fun createComponent(): JComponent {
        val settings = TestToSpecSettings.getInstance(project)

        specRootField = JBTextField(settings.state.specRootPath)
        annotationFqnField = JBTextField(settings.state.scenarioAnnotationFqn)
        capabilityAttrField = JBTextField(settings.state.capabilityAttribute)
        valueAttrField = JBTextField(settings.state.valueAttribute)

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Spec files root (relative to project):", specRootField!!)
            .addLabeledComponent("@Scenario annotation FQN:", annotationFqnField!!)
            .addLabeledComponent("Capability attribute name:", capabilityAttrField!!)
            .addLabeledComponent("Scenario name attribute:", valueAttrField!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = TestToSpecSettings.getInstance(project)
        return specRootField?.text != settings.state.specRootPath ||
                annotationFqnField?.text != settings.state.scenarioAnnotationFqn ||
                capabilityAttrField?.text != settings.state.capabilityAttribute ||
                valueAttrField?.text != settings.state.valueAttribute
    }

    override fun apply() {
        val settings = TestToSpecSettings.getInstance(project)
        settings.state.specRootPath = specRootField?.text ?: return
        settings.state.scenarioAnnotationFqn = annotationFqnField?.text ?: return
        settings.state.capabilityAttribute = capabilityAttrField?.text ?: return
        settings.state.valueAttribute = valueAttrField?.text ?: return
    }

    override fun reset() {
        val settings = TestToSpecSettings.getInstance(project)
        specRootField?.text = settings.state.specRootPath
        annotationFqnField?.text = settings.state.scenarioAnnotationFqn
        capabilityAttrField?.text = settings.state.capabilityAttribute
        valueAttrField?.text = settings.state.valueAttribute
    }
}
