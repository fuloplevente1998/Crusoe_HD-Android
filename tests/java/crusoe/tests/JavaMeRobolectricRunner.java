package crusoe.tests;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

/**
 * Robolectric normally delegates all javax.* classes to the host JVM.
 * Java ME runtime classes must instead share the Android sandbox classloader.
 * Otherwise they bind to Gradle's mockable android.jar, so no pixels are tested.
 */
public final class JavaMeRobolectricRunner extends RobolectricTestRunner {
    public JavaMeRobolectricRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override protected InstrumentationConfiguration createClassLoaderConfig(FrameworkMethod method) {
        return new JavaMeConfiguration(new InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method)));
    }

    private static final class JavaMeConfiguration extends InstrumentationConfiguration {
        JavaMeConfiguration(Builder b) {
            super(b.classNameTranslations, b.interceptedMethods, b.instrumentedPackages,
                    b.instrumentedClasses, b.classesToNotAcquire, b.packagesToNotAcquire,
                    b.classesToNotInstrument, b.packagesToNotInstrument, b.classesToNotInstrumentRegex);
        }

        @Override public boolean shouldAcquire(String name) {
            return name.startsWith("javax.microedition.") || super.shouldAcquire(name);
        }
    }
}
