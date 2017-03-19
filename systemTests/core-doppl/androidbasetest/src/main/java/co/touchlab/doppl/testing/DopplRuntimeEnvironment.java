package co.touchlab.doppl.testing;
import android.app.Application;

import java.lang.reflect.Field;

import co.touchlab.doppl.utils.PlatformUtils;

/**
 * Created by kgalligan on 6/18/16.
 */
public class DopplRuntimeEnvironment
{
    public static Application application;

    public static synchronized Application getApplication()
    {
        if(application == null)
        {
            if(PlatformUtils.isJ2objc())
            {
                throw new IllegalStateException("Context not set up. Use @RunWith(DopplContextTestRunner), or set up on your own.");
            }
            else
            {
                loadAndroid();
            }
        }

        return application;
    }

    public static void loadAndroid()
    {
        try
        {
            Class robo = DopplRuntimeEnvironment.class.forName("org.robolectric.RuntimeEnvironment");
            Field application = robo.getDeclaredField("application");
            DopplRuntimeEnvironment.application = (Application)application.get(null);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
