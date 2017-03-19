package android.content;
import android.os.Looper;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by kgalligan on 4/7/16.
 */
public class IOContextTest
{
    @Test
    public void customizedExceptionUsed() throws Throwable {
        try
        {
            Looper.prepareMainLooper();
            final IOSContext iosContext = new IOSContext();

            final SharedPreferences test = iosContext
                    .getSharedPreferences("test", Context.MODE_PRIVATE);

            test.edit().putString("asdf", "qwert").apply();

            Assert.assertEquals(test.getString("asdf", "fff"), "qwer");

            test.edit().remove("asdf").apply();

            Assert.assertEquals(test.getString("asdf", "fff"), "fff");

            final SharedPreferences.Editor edit = test.edit();
            edit.putBoolean("b", true);
            edit.putBoolean("b2", false);
            edit.putFloat("f", 1.235f);
            edit.putInt("i", 55543);
            edit.putLong("l", 492349238437l);
            edit.apply();

            Assert.assertEquals(test.getBoolean("b", false), true);
            Assert.assertEquals(test.getBoolean("b2", true), false);
            Assert.assertEquals(test.getBoolean("bnone", true), true);
            Assert.assertEquals(test.getBoolean("bnone2", false), false);
            Assert.assertEquals(test.getFloat("f", 1.11f), 1.235f);
            Assert.assertEquals(test.getInt("i", 33), 55543);
            Assert.assertEquals(test.getLong("l", 33), 492349238437l);
        }
        catch(Error e)
        {
            System.out.println(e.getMessage());
        }
    }
}
