package co.touchlab.doppl.testing;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by kgalligan on 5/29/16.
 */
@Target(ElementType.TYPE)
public @interface MockGen
{
    String[] classes();
}
