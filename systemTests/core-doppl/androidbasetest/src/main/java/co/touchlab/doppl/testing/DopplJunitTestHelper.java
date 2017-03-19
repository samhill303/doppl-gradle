package co.touchlab.doppl.testing;
import com.google.j2objc.annotations.AutoreleasePool;
import com.google.j2objc.annotations.WeakOuter;

import junit.runner.Version;

import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.JUnit4;
import org.junit.runners.Suite;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * Created by kgalligan on 8/8/16.
 */
public class DopplJunitTestHelper
{

    private static final String PROPERTIES_FILE_NAME = "JUnitTestRunner.properties";

    /**
     * Specifies the output format for tests.
     */
    public enum OutputFormat {
        JUNIT,            // JUnit style output.
        GTM_UNIT_TESTING  // Google Toolkit for Mac unit test output format.
    }

    /**
     * Specifies the sort order for tests.
     */
    public enum SortOrder {
        ALPHABETICAL,  // Sorted alphabetically
        RANDOM         // Sorted randomly (differs with each run)
    }

    /**
     * Specifies whether a pattern includes or excludes test classes.
     */
    public enum TestInclusion {
        INCLUDE,  // Includes test classes matching the pattern
        EXCLUDE   // Excludes test classes matching the pattern
    }

    private final PrintStream out;
    private final Set<String> includePatterns = new HashSet<>();
    private final Set<String> excludePatterns = new HashSet<>();
    private final Map<String, String> nameMappings = new HashMap<>();
    private final Map<String, String> randomNames = new HashMap<>();
    private final Random random = new Random(System.currentTimeMillis());
    private OutputFormat outputFormat = OutputFormat.JUNIT;
    private SortOrder sortOrder = SortOrder.ALPHABETICAL;

    public DopplJunitTestHelper() {
        this(System.out);
    }

    public DopplJunitTestHelper(PrintStream out) {
        this.out = out;
    }

    public static int main(String[] args) {
        // Create JUnit test runner.
        DopplJunitTestHelper runner = new DopplJunitTestHelper();
        runner.loadPropertiesFromResource(PROPERTIES_FILE_NAME);
        return runner.run();
    }

    /*public static int runMethod(Class clazz, String methodName, RunListener listener){
        JUnitCore junitCore = new JUnitCore();

        junitCore.addListener(listener);
        Request request = Request.classes(new Computer(), clazz);
        Description desiredDescription = Description.createTestDescription(clazz, methodName);
        request = request.filterWith(desiredDescription);

        Result result = junitCore.run(request);
        List<ResultContainer> resultList = new ArrayList<>(1);
        resultList.add(new ResultContainer(result, clazz));
        return runInner(resultList, !result.wasSuccessful());
    }*/

    public static int run(String[] classes)
    {
        return run(classes, null, null);
    }

    public static int run(String[] classes, RunListener listener)
    {
        return run(classes, listener, null);
    }

    /**
     * Runs the test classes given in {@param classes}.
     * @returns Zero if all tests pass, non-zero otherwise.
     */
    public static int run(String[] classes, RunListener listener, DopplJunitListener dopplListener)
    {
        try
        {
            JUnitCore junitCore = new JUnitCore();

            if(listener != null)
                junitCore.addListener(listener);

            boolean hasError = false;
            Result result;

            List<ResultContainer> resultList = new ArrayList<>(classes.length);
            for (String c : classes) {

                System.out.println("\n\n********** Running "+ c +" **********");

                if(dopplListener != null)
                    dopplListener.startRun(c);

                result = runSpecificTest(junitCore, c);

                if(dopplListener != null)
                    dopplListener.endRun(c);

                resultList.add(new ResultContainer(result, c));
                hasError = hasError || !result.wasSuccessful();
            }

            return runInner(resultList, hasError);
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    @AutoreleasePool
    public static Result runSpecificTest(JUnitCore junitCore, String c) throws ClassNotFoundException
    {
        Result result;
        if(c.contains("#"))
        {
            String[] split = c.split("#");
            Class clazz = Class.forName(split[0]);
            Request request = Request.classes(new Computer(), clazz);
            Description desiredDescription = Description.createTestDescription(clazz, split[1]);
            request = request.filterWith(desiredDescription);
            result = junitCore.run(request);
        }
        else
        {
            Class clazz = Class.forName(c);
            result = junitCore.run(clazz);
        }
        return result;
    }

    public interface DopplJunitListener
    {
        void startRun(String run);
        void endRun(String run);
    }

    private static int runInner(List<ResultContainer> resultList, boolean hasError){

        int bigTotal = 0;
        int failureTotal = 0;

        Collections.sort(resultList, new Comparator<ResultContainer>()
        {
            @Override
            public int compare(ResultContainer a, ResultContainer b)
            {
                if(a.result.wasSuccessful() == b.result.wasSuccessful())
                {
                    return a.testClassName.compareTo(b.testClassName);
                }
                else
                {
                   return a.result.wasSuccessful() ? -1 : 1;
                }
            }
        });

        for(ResultContainer result : resultList)
        {
            System.out.println("\n\n********** Result for "+ result.testClassName +" **********");
            if(result.result.wasSuccessful()) {
                System.out.println("Success");
            } else {
                failureTotal += result.result.getFailureCount();
                System.out.println("Failures "+ result.result.getFailureCount());
                List<Failure> failures = result.result.getFailures();
                for(Failure failure : failures) {
                    System.out.println(failure.toString());
                    System.out.println("message: " + failure.getMessage());
                    System.out.println("description: " + failure.getDescription().toString());
                    System.out.println("trace: " + failure.getTrace());
                }
            }
            bigTotal += result.result.getRunCount();
        }

        System.out.println("Total: "+ bigTotal);
        System.out.println("Failures: "+ failureTotal);

        return hasError ? 1 : 0;
    }

    static class ResultContainer
    {
        final Result result;
        final String testClassName;

        public ResultContainer(Result result, String testClassName)
        {
            this.result = result;
            this.testClassName = testClassName;
        }
    }

    /**
     * Runs the test classes that match settings in {@link #PROPERTIES_FILE_NAME}.
     * @returns Zero if all tests pass, non-zero otherwise.
     */
    public int run() {
        Set<Class> classesSet = getTestClasses();
        Class[] classes = classesSet.toArray(new Class[classesSet.size()]);
        sortClasses(classes, sortOrder);
        RunListener listener = newRunListener(outputFormat);
        return run(classes, listener, null);
    }

    public static int run(Class[] classes)
    {
        return run(classes, null, null);
    }

    public static int run(Class[] classes, RunListener listener)
    {
        return run(classes, listener, null);
    }

    public static int run(Class[] classes, RunListener listener, DopplJunitListener dopplJunitListener)
    {
        List<String> classnameList = new ArrayList<>();
        for(Class cl : classes)
        {
            classnameList.add(cl.getName());
        }
        return run(classnameList.toArray(new String[classnameList.size()]), listener, dopplJunitListener);
    }



    /**
     * Returns a new {@link RunListener} instance for the given {@param outputFormat}.
     */
    public RunListener newRunListener(OutputFormat outputFormat) {
        switch (outputFormat) {
            case JUNIT:
                out.println("JUnit version " + Version.id());
                return new TextListener(out);
            case GTM_UNIT_TESTING:
                return new GtmUnitTestingTextListener();
            default:
                throw new IllegalArgumentException("outputFormat");
        }
    }

    /**
     * Sorts the classes given in {@param classes} according to {@param sortOrder}.
     */
    public void sortClasses(Class[] classes, final SortOrder sortOrder) {
        Arrays.sort(classes, new Comparator<Class>() {
            public int compare(Class class1, Class class2) {
                String name1 = getSortKey(class1, sortOrder);
                String name2 = getSortKey(class2, sortOrder);
                return name1.compareTo(name2);
            }
        });
    }

    private String replaceAll(String value) {
        for (Map.Entry<String, String> entry : nameMappings.entrySet()) {
            String pattern = entry.getKey();
            String replacement = entry.getValue();
            value = value.replaceAll(pattern, replacement);
        }
        return value;
    }

    private String getSortKey(Class cls, SortOrder sortOrder) {
        String className = cls.getName();
        switch (sortOrder) {
            case ALPHABETICAL:
                return replaceAll(className);
            case RANDOM:
                String sortKey = randomNames.get(className);
                if (sortKey == null) {
                    sortKey = Integer.toString(random.nextInt());
                    randomNames.put(className, sortKey);
                }
                return sortKey;
            default:
                throw new IllegalArgumentException("sortOrder");
        }
    }

  /*-[
  // Returns true if |cls| conforms to the NSObject protocol.
  BOOL IsNSObjectClass(Class cls) {
    while (cls != nil) {
      if (class_conformsToProtocol(cls, @protocol(NSObject))) {
        return YES;
      }
      // class_conformsToProtocol() does not examine superclasses.
      cls = class_getSuperclass(cls);
    }
    return NO;
  }
  ]-*/

    /**
     * Returns the set of all loaded JUnit test classes.
     */
    private native Set<Class> getAllTestClasses() /*-[
    int classCount = objc_getClassList(NULL, 0);
    Class *classes = (Class *)malloc(classCount * sizeof(Class));
    objc_getClassList(classes, classCount);
    id<JavaUtilSet> result = AUTORELEASE([[JavaUtilHashSet alloc] init]);
    for (int i = 0; i < classCount; i++) {
      @try {
        Class cls = classes[i];
        if (IsNSObjectClass(cls)) {
          IOSClass *javaClass = IOSClass_fromClass(cls);
          if ([self isJUnitTestClassWithIOSClass:javaClass]) {
            [result addWithId:javaClass];
          }
        }
      }
      @catch (NSException *e) {
        // Ignore any exceptions thrown by class initialization.
      }
    }
    free(classes);
    return result;
  ]-*/;

    /**
     * @return true if {@param cls} is either a JUnit 3 or JUnit 4 test.
     */
    protected boolean isJUnitTestClass(Class cls) {
        return isJUnit3TestClass(cls) || isJUnit4TestClass(cls);
    }

    /**
     * @return true if {@param cls} derives from {@link Test} and is not part of the
     * {@link junit.framework} package.
     */
    protected boolean isJUnit3TestClass(Class cls) {
        if (Test.class.isAssignableFrom(cls)) {
            String packageName = getPackageName(cls);
            return !packageName.startsWith("junit.framework")
                    && !packageName.startsWith("junit.extensions");
        }
        return false;
    }

    /**
     * @return true if {@param cls} is {@link JUnit4} annotated.
     */
    protected boolean isJUnit4TestClass(Class cls) {
        // Need to find test classes, otherwise crashes with b/11790448.
        if (!cls.getName().endsWith("Test")) {
            return false;
        }
        // Check the annotations.
        Annotation annotation = cls.getAnnotation(RunWith.class);
        if (annotation != null) {
            RunWith runWith = (RunWith) annotation;
            Object value = runWith.value();
            if (value.equals(JUnit4.class) || value.equals(Suite.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of a class's package or "" for the default package
     * or (for Foundation classes) no package object.
     */
    private String getPackageName(Class cls) {
        Package pkg = cls.getPackage();
        return pkg != null ? pkg.getName() : "";
    }

    /**
     * Returns the set of test classes that match settings in {@link #PROPERTIES_FILE_NAME}.
     */
    private Set<Class> getTestClasses() {
        Set<Class> allTestClasses = getAllTestClasses();
        Set<Class> includedClasses = new HashSet<>();

        if (includePatterns.isEmpty()) {
            // Include all tests if no include patterns specified.
            includedClasses = allTestClasses;
        } else {
            // Search all tests for tests to include.
            for (Class testClass : allTestClasses) {
                for (String includePattern : includePatterns) {
                    if (matchesPattern(testClass, includePattern)) {
                        includedClasses.add(testClass);
                        break;
                    }
                }
            }
        }

        // Search included tests for tests to exclude.
        Iterator<Class> includedClassesIterator = includedClasses.iterator();
        while (includedClassesIterator.hasNext()) {
            Class testClass = includedClassesIterator.next();
            for (String excludePattern : excludePatterns) {
                if (matchesPattern(testClass, excludePattern)) {
                    includedClassesIterator.remove();
                    break;
                }
            }
        }

        return includedClasses;
    }

    private boolean matchesPattern(Class testClass, String pattern) {
        return testClass.getCanonicalName().contains(pattern);
    }

    private void loadProperties(InputStream stream) {
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            onError(e);
        }
        Set<String> propertyNames = properties.stringPropertyNames();
        for (String key : propertyNames) {
            String value = properties.getProperty(key);
            try {
                if (key.equals("outputFormat")) {
                    outputFormat = OutputFormat.valueOf(value);
                } else if (key.equals("sortOrder")) {
                    sortOrder = SortOrder.valueOf(value);
                } else if (value.equals(TestInclusion.INCLUDE.name())) {
                    includePatterns.add(key);
                } else if (value.equals(TestInclusion.EXCLUDE.name())) {
                    excludePatterns.add(key);
                } else {
                    nameMappings.put(key, value);
                }
            } catch (IllegalArgumentException e) {
                onError(e);
            }
        }
    }

    private void loadPropertiesFromResource(String resourcePath) {
        try {
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
            if (stream != null) {
                loadProperties(stream);
            } else {
                throw new IOException(String.format("Resource not found: %s", resourcePath));
            }
        } catch (Exception e) {
            onError(e);
        }
    }

    private void onError(Exception e) {
        e.printStackTrace(out);
    }

    @WeakOuter
    private class GtmUnitTestingTextListener extends RunListener
    {

        private int numTests = 0;
        private int numFailures = 0;
        private final int numUnexpected = 0; // Never changes, but required in output.

        private Failure testFailure;
        private double  testStartTime;

        @Override
        public void testRunFinished(Result result) throws Exception {
            out.printf("Executed %d tests, with %d failures (%d unexpected)\n", numTests, numFailures,
                    numUnexpected);
        }

        @Override
        public void testStarted(Description description) throws Exception {
            numTests++;
            testFailure = null;
            testStartTime = System.currentTimeMillis();
            out.printf("Test Case '-[%s]' started.\n", parseDescription(description));
        }

        @Override
        public void testFinished(Description description) throws Exception {
            double testEndTime = System.currentTimeMillis();
            double elapsedSeconds = 0.001 * (testEndTime - testStartTime);
            String statusMessage = "passed";
            if (testFailure != null) {
                statusMessage = "failed";
                out.print(testFailure.getTrace());
            }
            out.printf("Test Case '-[%s]' %s (%.3f seconds).\n\n",
                    parseDescription(description), statusMessage, elapsedSeconds);
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            testFailure = failure;
            numFailures++;
        }

        private String parseDescription(Description description) {
            String displayName = description.getDisplayName();
            int p1 = displayName.indexOf("(");
            int p2 = displayName.indexOf(")");
            if (p1 < 0 || p2 < 0 || p2 <= p1) {
                return displayName;
            }
            String methodName = displayName.substring(0, p1);
            String className = displayName.substring(p1 + 1, p2);
            return replaceAll(className) + " " + methodName;
        }
    }
}
