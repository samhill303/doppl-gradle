package co.touchlab.doppl.testing;
import android.support.annotation.NonNull;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.lang.reflect.Constructor;

import co.touchlab.doppl.utils.PlatformUtils;

/**
 * Created by kgalligan on 5/30/16.
 */
public class DopplRobolectricTestRunner extends Runner
{
    private final Runner delegateRunner;

    public DopplRobolectricTestRunner(final Class<?> testClass) throws InitializationError
    {
        this.delegateRunner = loadPlatformSpecificRunner(testClass, "org.robolectric.RobolectricTestRunner");
    }

    @NonNull
    public static Runner loadPlatformSpecificRunner(Class<?> testClass, String className) throws InitializationError
    {
        Runner delegateRunner;
        if(PlatformUtils.isJ2objc())
        {
            BlockJUnit4ClassRunner blockJUnit4ClassRunner = new DopplContextTestRunner(testClass);

            delegateRunner = blockJUnit4ClassRunner;

        }
        else
        {
            try
            {
                Class<?> runnerClass = DopplRobolectricTestRunner.class.forName(className);
                Constructor constructor = runnerClass.getConstructors()[0];
                delegateRunner = (Runner)constructor.newInstance(testClass);
            }
            catch(Exception e)
            {
                throw new RuntimeException("RobolectricTestRunner cannot be instantiated", e);
            }
        }
        return delegateRunner;
    }

    /*
     * (non-Javadoc)
     * @see org.junit.runner.Describable#getDescription()
     */
    public Description getDescription()
    {
        return delegateRunner.getDescription();
    }

    /**
     * Run the tests for this runner.
     *
     * @param notifier will be notified of events while tests are being run--tests being
     * started, finishing, and failing
     */
    public void run(RunNotifier notifier)
    {
        delegateRunner.run(notifier);
    }



    /*@Override
    public void run(RunNotifier notifier)
    {
        if(PlatformUtils.isJ2objc())
        {
            System.out.println("Running: "+ getTestClass().getJavaClass().getName());
            notifier.addListener(new RunListener(){
                @Override
                public void testRunStarted(Description description) throws Exception
                {
                    System.out.println("testRunStarted: "+ description.getMethodName() +"/"+ description.getDisplayName());
                }
            });
            super.run(notifier);
        }
    }

    @Override
    protected void validateInstanceMethods(List<Throwable> errors)
    {
        if(PlatformUtils.isJ2objc())
            super.validateInstanceMethods(errors);
        else
        {
            validatePublicVoidNoArgMethods(After.class, false, errors);
            validatePublicVoidNoArgMethods(Before.class, false, errors);
            validateTestMethods(errors);
        }
    }*/
}
