package co.touchlab.doppl.testing;
import android.content.IOSContext;
import android.os.Looper;

import java.io.File;


/**
 * Created by kgalligan on 3/21/16.
 */
public class TestingContext extends IOSContext
{
    private final File rootDir;

    public TestingContext(File rootDir)
    {
        this.rootDir = rootDir;
        if(Looper.getMainLooper() == null)
            Looper.prepareMainLooper();
    }

    @Override
    public File getRootDir()
    {
        rootDir.mkdirs();
        return rootDir;
    }
}
