package co.touchlab.doppl.testing.mockgen;
import com.squareup.javapoet.TypeName;

/**
 * Created by kgalligan on 8/7/16.
 */
public class ParameterWrapper
{
    final TypeName typeName;
    final String name;

    public ParameterWrapper(String name, TypeName typeName)
    {
        this.name = name;
        this.typeName = typeName;
    }

    public TypeName getTypeName()
    {
        return typeName;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "ParameterWrapper{" +
                "typeName=" + typeName +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }

        ParameterWrapper that = (ParameterWrapper) o;

        return typeName != null ? typeName.equals(that.typeName) : that.typeName == null;

    }

    @Override
    public int hashCode()
    {
        return typeName != null ? typeName.hashCode() : 0;
    }
}
