package co.touchlab.doppl.testing;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.notification.RunListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import co.touchlab.doppl.testing.mockgen.MethodWrapper;
import co.touchlab.doppl.testing.mockgen.ParameterWrapper;


/**
 * Created by kgalligan on 6/13/16.
 */
public class TestAnnotationProcessor extends AbstractProcessor
{
    public static final String HANDLER_FIELD_NAME = "$__handler";

    private Types    typeUtils;
    private Filer    filer;
    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
            messager.printMessage(Diagnostic.Kind.NOTE, "Is running");
            return safeProcess(roundEnv);
        }
        catch(Exception e)
        {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "(Failed in annotation processing) " + e.getClass().getName() + "/" +
                            e.getMessage() + "\n\n");
            throw new RuntimeException(e);
        }
    }

    private boolean safeProcess(RoundEnvironment roundEnv)
    {
        Set<? extends Element> matchingElements = roundEnv.getElementsAnnotatedWith(ElementTreeDebug.class);
        for(Element element : matchingElements)
        {
            List<String> lines = new ArrayList<>();
            printElement(element, "/", lines);
            System.out.println(StringUtils.join(lines, "\n"));
        }
        if(mockGenProcess(roundEnv))
        {
            return false;
        }
        return dopplTestProcess(roundEnv);
    }

    private void printElement(Element element, String prefix, List<String> lines)
    {
        lines.add(prefix + " [" + element.getKind() + "] - " + element.getSimpleName().toString());
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for(Element childElement : enclosedElements)
        {
            printElement(childElement, prefix + "--/", lines);
        }
    }

    private String stripGenericsInType(String type)
    {
        StringBuilder sb = new StringBuilder();
        int countAngles = 0;
        for(int i=0; i<type.length(); i++)
        {
            char c = type.charAt(i);
            if(c == '<')
                countAngles++;

            if(countAngles == 0)
                sb.append(c);

            if(c == '>')
                countAngles--;

        }

        return sb.toString();
    }

    private boolean mockGenProcess(RoundEnvironment roundEnv)
    {
        Set<? extends Element> matchingElements = roundEnv.getElementsAnnotatedWith(MockGen.class);
        Set<String> classnamesToMock = new HashSet<>();

        for(Element element : matchingElements)
        {
            TypeElement typeElement = (TypeElement) element;
            List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();

            String[] mockClasses = typeElement.getAnnotation(MockGen.class).classes();
            for(String cl : mockClasses)
            {
                classnamesToMock.add(cl);
            }
        }

        for(String moxyTypeString : classnamesToMock)
        {
            TypeElement moxyTypeElement1 = elementUtils.getTypeElement(moxyTypeString);
            ClassName className = ClassName.get(moxyTypeElement1);
            ClassName moxyName = ClassName.get(className.packageName(),
                    StringUtils.join(className.simpleNames(), "__") + "$Moxy");

            TypeSpec.Builder configBuilder = TypeSpec
                    .classBuilder(moxyName.simpleName())

                    .addModifiers(Modifier.PUBLIC)
                    .superclass(className)
                    .addField(InvocationHandler.class, HANDLER_FIELD_NAME)
                    //                        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(GeneratedTableMapper.class), className))
                    .addJavadoc("Generated on $L\n",
                            new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date()));

            generateConstructorsAndFields(configBuilder, moxyTypeElement1);

            MethodWrapper[] methodsToProxy = getMethodsToProxyRecursive(moxyTypeElement1, null);

            for(MethodWrapper method : methodsToProxy)
            {
                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getName());
                MethodSpec.Builder superBuilder = MethodSpec.methodBuilder(
                        "super$" + method.getName());

                if(method.getAccessLevel() != null)
                {
                    methodBuilder.addModifiers(method.getAccessLevel());
                    superBuilder.addModifiers(method.getAccessLevel());
                }

                if(method.getThrownTypes() != null && method.getThrownTypes().size() > 0)
                {
                    for(TypeMirror throwMirror : method.getThrownTypes())
                    {
                        methodBuilder.addException(TypeName.get(throwMirror));
                        superBuilder.addException(TypeName.get(throwMirror));
                    }
                }

                TypeName returnType = method.getReturnType();
                boolean voidReturn = returnType.equals(TypeName.VOID);
                if(! voidReturn)
                {
                    returnType = strippedType(returnType);
                    methodBuilder.returns(returnType);
                    superBuilder.returns(returnType);
                }

                List<String> paramStrings = new ArrayList<>();
                List<String> typeStrings = new ArrayList<>();

                for(ParameterWrapper parameter : method.getParameters())
                {
                    TypeName strippedTypeName = strippedType(parameter.getTypeName());

                    methodBuilder.addParameter(strippedTypeName, parameter.getName());
                    superBuilder.addParameter(strippedTypeName, parameter.getName());
                    paramStrings.add(parameter.getName());
                    typeStrings.add(strippedTypeName.toString() + ".class");
                }

                String directCallString = makeDirectCall(method, voidReturn, paramStrings);

                StringBuilder handlerCall = new StringBuilder();
                if(! voidReturn)
                {
                    handlerCall.append("return (" + strippedType(method.getReturnType()).toString() + ")");
                }
                handlerCall.append("$" + HANDLER_FIELD_NAME)
                        .append(".invoke(this, getClass().getMethod(\"")
                        .append(method.getName())
                        .append("\", new Class[]{")
                        .append(StringUtils.join(typeStrings, ", "))
                        .append("}), new Object[]{")
                        .append(StringUtils.join(paramStrings, ", "))
                        .append("})");

                methodBuilder.beginControlFlow("try");
                methodBuilder.beginControlFlow("if($" + HANDLER_FIELD_NAME + " == null)");


                methodBuilder.addStatement(directCallString)
                        .nextControlFlow("else")
                        .addStatement(handlerCall.toString())
                        .endControlFlow();

                methodBuilder.nextControlFlow("catch(Throwable __ttlive)");
                methodBuilder.beginControlFlow("if(__ttlive instanceof RuntimeException)")
                        .addStatement("throw (RuntimeException)__ttlive");

                List<? extends TypeMirror> thrownTypes = method.getThrownTypes();

                List<TypeMirror> checkedTypes = new ArrayList<>();
                checkedTypes.add(elementUtils.getTypeElement("java.lang.RuntimeException").asType());

                for(TypeMirror thrownType : thrownTypes)
                {
                    boolean alreadyChecked = false;
                    for(TypeMirror checkedType : checkedTypes)
                    {
                        if(typeUtils.isAssignable(thrownType, checkedType))
                        {
                            alreadyChecked = true;
                            break;
                        }
                    }
                    if(alreadyChecked)
                        continue;


                    methodBuilder.nextControlFlow("else if(__ttlive instanceof $T)", thrownType)
                            .addStatement("throw ($T)__ttlive", thrownType);

                    checkedTypes.add(thrownType);
                }
                methodBuilder
                        .nextControlFlow("else")
                        .addStatement("throw new RuntimeException(__ttlive)")
                        .endControlFlow();
                methodBuilder.endControlFlow();

                configBuilder.addMethod(methodBuilder.build());

                superBuilder.addStatement(directCallString);

                configBuilder.addMethod(superBuilder.build());
            }
            JavaFile javaFile = JavaFile.builder(className.packageName(), configBuilder.build())
                    .build();

            try
            {
                javaFile.writeTo(filer);
            }
            catch(IOException e)
            {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                printWriter.close();
                messager.printMessage(Diagnostic.Kind.ERROR, "Code gen failed: " + e +"\n"+ stringWriter.toString());
                return true;
            }
        }

        return false;
    }

    private TypeName strippedType(TypeName typeName)
    {
        TypeName strippedTypeName;
        if(typeName.isPrimitive())
        {
            strippedTypeName = typeName;
        }
        else
        {
            try
            {
                String typeNameString = stripGenericsInType(typeName.toString());
                strippedTypeName = ClassName.bestGuess(typeNameString);
            }
            catch(Exception e)
            {
                //This is ugly, but its an ugly world sometimes. For primitive arrays, at least one time.
                messager.printMessage(Diagnostic.Kind.WARNING, e.getMessage());
                strippedTypeName = typeName;
            }
        }
        return strippedTypeName;
    }

    private String makeDirectCall(MethodWrapper method, boolean voidReturn, List<String> paramStrings)
    {
        StringBuilder directCall = new StringBuilder();
        if(method.isAbstractMethod())
        {
            directCall.append("throw new UnsupportedOperationException(\""+ method.getName() +" is abstract\")");
        }
        else
        {
            if(! voidReturn)
            {
                directCall.append("return ");
            }
            directCall.append("super.").append(method.getName()).append("(");
            directCall.append(StringUtils.join(paramStrings, ", "));
            directCall.append(")");
        }

        return directCall.toString();
    }

    private void error(Element e, String msg, Object... args)
    {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    private <T, G extends T> void generateConstructorsAndFields(TypeSpec.Builder typeBuilder, TypeElement superClass)
    {
        List<? extends Element> enclosedElements = superClass.getEnclosedElements();
        for(Element element : enclosedElements)
        {
            if(element.getKind() == ElementKind.CONSTRUCTOR)
            {
                if(element.getModifiers().contains(Modifier.FINAL) ||
                        element.getModifiers().contains(Modifier.PRIVATE))
                {
                    continue;
                }

                MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();

                int paramCount = 0;
                List<String> paramNames = new ArrayList<>();

                ExecutableElement executableElement = (ExecutableElement) element;

                for(VariableElement param : executableElement.getParameters())
                {
                    String name = "a" + (paramCount++);
                    constructorBuilder.addParameter(strippedType(TypeName.get(param.asType())), name);
                    paramNames.add(name);
                }

                constructorBuilder.addStatement(
                        "super(" + StringUtils.join(paramNames, ", ") + ")");

                typeBuilder.addMethod(constructorBuilder.build());
            }
        }

        MethodSpec.Builder getHandler = MethodSpec.methodBuilder("getHandler");
        getHandler.addModifiers(Modifier.PUBLIC)
                .returns(InvocationHandler.class)
                .addStatement("return $$__handler");

        MethodSpec.Builder setHandler = MethodSpec.methodBuilder("setHandler");
        setHandler.addModifiers(Modifier.PUBLIC)
                .addParameter(InvocationHandler.class, "handler")
                .addStatement("$$__handler = handler");

        typeBuilder.addMethod(getHandler.build());
        typeBuilder.addMethod(setHandler.build());
    }

    // The type parameter on Constructor is the class in which the constructor is declared.
    // The getDeclaredConstructors() method gets constructors declared only in the given class,
    // hence this cast is safe.
    @SuppressWarnings("unchecked")
    private static <T> Constructor<T>[] getConstructorsToOverwrite(Class<T> clazz)
    {
        return (Constructor<T>[]) clazz.getDeclaredConstructors();
    }

    private MethodWrapper[] getMethodsToProxyRecursive(TypeElement baseClass, Set<TypeMirror> interfaces)
    {
        Set<MethodWrapper> methodsToProxy = new HashSet<MethodWrapper>();
        Set<MethodWrapper> seenFinalMethods = new HashSet<MethodWrapper>();
        for(TypeElement c = baseClass;
                c != null; c = (TypeElement) typeUtils.asElement(c.getSuperclass()))
        {
            getMethodsToProxy(methodsToProxy, seenFinalMethods, c);
        }

        return methodsToProxy.toArray(new MethodWrapper[methodsToProxy.size()]);
    }

    private void getMethodsToProxy(Set<MethodWrapper> sink, Set<MethodWrapper> seenFinalMethods, TypeElement c)
    {
        for(Element method : c.getEnclosedElements())
        {
            if(method.getKind() != ElementKind.METHOD)
            {
                continue;
            }

            ExecutableElement methodElement = (ExecutableElement) method;

            if(methodElement.getModifiers().contains(Modifier.FINAL))
            {
                // Skip final methods, we can't override them. We
                // also need to remember them, in case the same
                // method exists in a parent class.
                MethodWrapper entry = new MethodWrapper(methodElement, typeUtils);
                seenFinalMethods.add(entry);
                // We may have seen this method already, from an interface
                // implemented by a child class. We need to remove it here.
                sink.remove(entry);
                continue;
            }
            if(methodElement.getModifiers().contains(Modifier.STATIC))
            {
                // Skip static methods, overriding them has no effect.
                continue;
            }
            if(! methodElement.getModifiers().contains(Modifier.PUBLIC) &&
                    ! methodElement.getModifiers().contains(Modifier.PROTECTED))
            {
                // Skip private methods, since they are invoked through direct
                // invocation (as opposed to virtual). Therefore, it would not
                // be possible to intercept any private method defined inside
                // the proxy class except through reflection.

                // Skip package-private methods as well. The proxy class does
                // not actually inherit package-private methods from the parent
                // class because it is not a member of the parent's package.
                // This is even true if the two classes have the same package
                // name, as they use different class loaders.
                continue;
            }
            if(methodElement.getSimpleName().toString().equals("finalize") &&
                    methodElement.getParameters().size() == 0)
            {
                // Skip finalize method, it's likely important that it execute as normal.
                continue;
            }
            MethodWrapper entry = new MethodWrapper(methodElement, typeUtils);
            if(seenFinalMethods.contains(entry))
            {
                // This method is final in a child class.
                // We can't override it.
                continue;
            }
            sink.add(entry);
        }
    }

    private boolean dopplTestProcess(RoundEnvironment roundEnv)
    {
        Set<? extends Element> matchingElements = roundEnv.getElementsAnnotatedWith(DopplTest.class);
        if(matchingElements.size() == 0)
        {
            messager.printMessage(Diagnostic.Kind.NOTE, "No test matches");
            return false;
        }
        else
        {
            messager.printMessage(Diagnostic.Kind.NOTE, "matching " + matchingElements.size());
        }

        ClassName allTests = ClassName.get("", "AllTests");

        TypeSpec.Builder configBuilder = TypeSpec.classBuilder(allTests.simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        MethodSpec.Builder runAllTestsMethodBuilder =
                MethodSpec.methodBuilder("runAllTests")
                        .returns(int.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);


        StringBuilder sb = new StringBuilder();

        for(Element annotatedElement : matchingElements)
        {
            if(sb.length() > 0)
            {
                sb.append(",\n");
            }

            TypeElement typeElement = (TypeElement) annotatedElement;
            sb.append(typeElement.getQualifiedName().toString()).append(".class");
        }

        ArrayTypeName classArray = ArrayTypeName.of(Class.class);

        runAllTestsMethodBuilder.addStatement(
                "return $T.run(new $T $L, new $T())",
                DopplJunitTestHelper.class,
                classArray,
                "{"+ sb.toString() +"}", RunListener.class
                );

        configBuilder.addMethod(runAllTestsMethodBuilder.build());


        JavaFile javaFile = JavaFile.builder(allTests.packageName(), configBuilder.build()).build();

        try
        {
            javaFile.writeTo(filer);
        }
        catch(IOException e)
        {
            messager.printMessage(Diagnostic.Kind.ERROR, "Code gen failed: " + e);
            return false;
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        Set<String> annotations = new LinkedHashSet<String>();
        annotations.add(DopplTest.class.getCanonicalName());
        annotations.add(MockGen.class.getCanonicalName());
        annotations.add(ElementTreeDebug.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }
}
