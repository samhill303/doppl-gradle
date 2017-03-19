package co.touchlab.doppl.testing;
import android.support.annotation.NonNull;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.io.IOException;

/**
 * Created by kgalligan on 12/4/16.
 */

public class DopplContextTestRunner extends BlockJUnit4ClassRunner
{
    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public DopplContextTestRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }


    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier)
    {
        File contextDataDir = initContext();
        try
        {
            super.runChild(method, notifier);
        }
        finally
        {
            deleteRecursive(contextDataDir);
            contextDataDir.delete();
        }
    }

    @NonNull
    private File initContext()
    {
        try
        {
            File context = File.createTempFile("context", null);
            String filename = context.getName();
            if(!context.isDirectory() && filename.endsWith(".tmp"))
            {
                context = new File(context.getParentFile(), filename.substring(0, filename.lastIndexOf(".tmp")));
                if(context.exists())
                    throw new RuntimeException("Can't create a new temp file for testing context");
            }
            context.mkdirs();

            DopplRuntimeEnvironment.application = new TestingContext(context);

            return context;
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void deleteRecursive(File contextDataDir)
    {
        File[] files = contextDataDir.listFiles();
        for(File file : files)
        {
            if(file.isDirectory())
                deleteRecursive(file);

            file.delete();
        }
    }
}
