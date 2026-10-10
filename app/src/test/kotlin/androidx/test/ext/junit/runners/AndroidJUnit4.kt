package androidx.test.ext.junit.runners

import androidx.test.core.app.ApplicationProvider
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.robolectric.annotation.Config

class AndroidJUnit4(klass: Class<*>) : BlockJUnit4ClassRunner(klass) {
    override fun methodBlock(method: FrameworkMethod): Statement {
        val base = super.methodBlock(method)
        val methodConfig = method.getAnnotation(Config::class.java)
        val classConfig = testClass.javaClass.getAnnotation(Config::class.java)
        val qualifier = methodConfig?.qualifiers?.takeIf { it.isNotBlank() }
            ?: classConfig?.qualifiers?.takeIf { it.isNotBlank() }
        return object : Statement() {
            override fun evaluate() {
                ApplicationProvider.currentQualifier.set(qualifier)
                try {
                    base.evaluate()
                } finally {
                    ApplicationProvider.currentQualifier.remove()
                }
            }
        }
    }
}
