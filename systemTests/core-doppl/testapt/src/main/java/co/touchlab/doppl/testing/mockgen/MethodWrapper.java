package co.touchlab.doppl.testing.mockgen;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

/**
 * Created by kgalligan on 8/7/16.
 */
public class MethodWrapper
{
    final String                 name;
    final Modifier accessLevel;
    final TypeName               returnType;
    final List<ParameterWrapper> parameters;
    private final List<? extends TypeMirror> thrownTypes;
    private final boolean abstractMethod;

   /* public MethodWrapper(Method method)
    {
        name = method.getName();
        returnType = TypeName.get(method.getReturnType());
        parameters = new ArrayList<>();
        for(Parameter p : method.getParameters())
        {
            parameters.add(new ParameterWrapper(p.getName(), TypeName.get(p.getType())));
        }
    }*/

    public MethodWrapper(ExecutableElement executableElement, Types types)
    {
        thrownTypes = executableElement.getThrownTypes();
        name = executableElement.getSimpleName().toString();
        if(executableElement.getModifiers().contains(Modifier.PRIVATE))
            accessLevel = Modifier.PRIVATE;
        else if(executableElement.getModifiers().contains(Modifier.PROTECTED))
            accessLevel = Modifier.PROTECTED;
        else if(executableElement.getModifiers().contains(Modifier.PUBLIC))
            accessLevel = Modifier.PUBLIC;
        else
            accessLevel = null;

        abstractMethod = executableElement.getModifiers().contains(Modifier.ABSTRACT);

        TypeMirror returnType = executableElement.getReturnType();

        try
        {
            this.returnType = needRealTypes(returnType, types);

            parameters = new ArrayList<>();
            for(VariableElement p : executableElement.getParameters())
            {
                parameters.add(new ParameterWrapper(p.getSimpleName().toString(), needRealTypes(p.asType(), types)));
            }
        }
        catch(Exception e)
        {
            System.out.println("executableElement: "+ executableElement.getEnclosingElement().getSimpleName() +"/"+ executableElement.getSimpleName());
            System.out.println("Reading field failed: "+ name);
            e.printStackTrace();
            if(e instanceof RuntimeException)
                throw (RuntimeException)e;
            else
                throw new RuntimeException(e);
        }
    }

    private TypeName needRealTypes(TypeMirror mirror, Types types)
    {
        if(mirror.getKind() == TypeKind.INT ||
                mirror.getKind() == TypeKind.BOOLEAN ||
                mirror.getKind() == TypeKind.BYTE ||
                mirror.getKind() == TypeKind.CHAR ||
                mirror.getKind() == TypeKind.DOUBLE ||
                mirror.getKind() == TypeKind.FLOAT ||
                mirror.getKind() == TypeKind.LONG ||
                mirror.getKind() == TypeKind.SHORT ||
                mirror.getKind() == TypeKind.VOID ||
                mirror.getKind() == TypeKind.ARRAY
                )
            return TypeName.get(mirror);


        Element element = types.asElement(mirror);
        if(element.getKind() == ElementKind.TYPE_PARAMETER)
        {
            System.out.println("mirror.getClass(): "+ mirror.getClass() +"/mirror.getKind(): "+ mirror.getKind());

            if(mirror instanceof TypeVariable)
            {
                TypeMirror extendsBound = ((TypeVariable) mirror).getUpperBound();

                if(extendsBound == null)
                    return TypeName.get(java.lang.Object.class);
                else
                    return TypeName.get(extendsBound);
            }
            else
            {
                return TypeName.get(java.lang.Object.class);
            }
        }
        else
        {
            return TypeName.get(mirror);
        }
    }

    public List<? extends TypeMirror> getThrownTypes()
    {
        return thrownTypes;
    }

    public Modifier getAccessLevel()
    {
        return accessLevel;
    }

    public boolean isAbstractMethod()
    {
        return abstractMethod;
    }

    public String getName()
    {
        return name;
    }

    public TypeName getReturnType()
    {
        return returnType;
    }

    public List<ParameterWrapper> getParameters()
    {
        return parameters;
    }

    @Override
    public String toString()
    {
        return "MethodWrapper{" +
                "name='" + name + '\'' +
                ", returnType=" + returnType +
                ", parameters=" + parameters +
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

        MethodWrapper that = (MethodWrapper) o;

        if(name != null ? ! name.equals(that.name) : that.name != null)
        {
            return false;
        }
        if(returnType != null ? ! returnType.equals(that.returnType) : that.returnType != null)
        {
            return false;
        }
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;

    }

    @Override
    public int hashCode()
    {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}
