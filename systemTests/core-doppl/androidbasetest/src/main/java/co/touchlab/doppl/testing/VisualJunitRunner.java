package co.touchlab.doppl.testing;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.List;

/**
 * Created by kgalligan on 7/23/16.
 */
public abstract class VisualJunitRunner
{
    private void runAllInBackground()
    {

        JUnitCore junit = new JUnitCore();

        Result result = runTestsByMyself(junit);

        System.out.println("Junit complete");
        if(result.wasSuccessful())
        {
            System.out.println("Success");
        }
        else
        {
            List<Failure> failures = result.getFailures();
            for(Failure failure : failures)
            {
                System.out.println(failure.toString());
                System.out.println("message: " + failure.getMessage());
                System.out.println("description: " + failure.getDescription().toString());
                System.out.println("trace: " + failure.getTrace());
            }
            System.out.println("Failures " + result.getFailureCount());

        }

        System.out.println("Total " + result.getRunCount());
    }

    private Result runTestsByMyself(JUnitCore junit)
    {
        return junit.run(testClasses());
    }

    protected abstract Class[] testClasses();

    public void goRun()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                runAllInBackground();
            }
        }.start();
    }
}
