package co.touchlab.doppl.testing;
import org.junit.runners.Parameterized;

import java.io.File;

import co.touchlab.doppl.utils.PlatformUtils;

/**
 * Created by kgalligan on 8/24/16.
 */
public class DopplParameterizedRobolectricTestRunner extends Parameterized {

    /**
     * Only called reflectively. Do not use programmatically.
     *
     * @param klass
     */
    public DopplParameterizedRobolectricTestRunner(Class<?> klass) throws Throwable
    {
        super(klass);
        if(PlatformUtils.isJ2objc())
        {
            File rootDir = new File("/Users/kgalligan/temp/test_" + System.currentTimeMillis());
            rootDir.mkdirs();
            DopplRuntimeEnvironment.application = new TestingContext(rootDir);
        }
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
    }*/

}