package ewc.openspec.testlink

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Scenario(val capability: String, val value: String)
