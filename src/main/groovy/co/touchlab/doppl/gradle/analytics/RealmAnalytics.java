package co.touchlab.doppl.gradle.analytics;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Created by kgalligan on 4/7/17.
 */
public class RealmAnalytics {
    private static RealmAnalytics instance;
    private static final int READ_TIMEOUT = 2000;
    private static final int CONNECT_TIMEOUT = 4000;
    private static final String ADDRESS_PREFIX = "https://api.mixpanel.com/track/?data=";
    private static final String ADDRESS_SUFFIX = "&ip=1";
    private static final String TOKEN = "a6b86f2bfca54adc8a9a5dbe28d615b5";
    private static final String EVENT_NAME = "Run";
    private static final String JSON_TEMPLATE
            = "{\n"
            + "   \"event\": \"%EVENT%\",\n"
            + "   \"properties\": {\n"
            + "      \"token\": \"%TOKEN%\",\n"
            + "      \"distinct_id\": \"%USER_ID%\",\n"
            + "      \"Anonymized MAC Address\": \"%USER_ID%\",\n"
            + "      \"Anonymized Bundle ID\": \"%APP_ID%\",\n"
            + "      \"Binding\": \"java\",\n"
            + "      \"Language\": \"%LANGUAGE%\",\n"
            + "      \"Realm Version\": \"%REALM_VERSION%\",\n"
            + "      \"Host OS Type\": \"%OS_TYPE%\",\n"
            + "      \"Host OS Version\": \"%OS_VERSION%\",\n"
            + "      \"Target OS Type\": \"android\"\n"
            + "   }\n"
            + "}";

    // The list of packages the model classes reside in
    private Set<String> packages;

    private boolean usesKotlin;
    private boolean usesSync;

    public RealmAnalytics() {
//        this.packages = packages;
//        this.usesKotlin = usesKotlin;
//        this.usesSync = usesSync;
    }

    private void send() {
        try {
            URL url = getUrl();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            connection.getResponseCode();
        } catch (Exception ignored) {
        }
    }

    public void execute() {
        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                send();
            }
        });
        backgroundThread.start();
        try {
            backgroundThread.join(CONNECT_TIMEOUT + READ_TIMEOUT);
        } catch (InterruptedException ignored) {
            // We ignore this exception on purpose not to break the build system if this class fails
        } catch (IllegalArgumentException ignored) {
            // We ignore this exception on purpose not to break the build system if this class fails
        }
    }

    public URL getUrl() throws
            MalformedURLException,
            SocketException,
            NoSuchAlgorithmException,
            UnsupportedEncodingException {
        return new URL(ADDRESS_PREFIX + Utils.base64Encode(generateJson()) + ADDRESS_SUFFIX);
    }

    public String generateJson() throws SocketException, NoSuchAlgorithmException {
        return JSON_TEMPLATE
                .replaceAll("%EVENT%", EVENT_NAME)
                .replaceAll("%TOKEN%", TOKEN)
                .replaceAll("%USER_ID%", ComputerIdentifierGenerator.get())
                .replaceAll("%APP_ID%", getAnonymousAppId())
                .replaceAll("%LANGUAGE%", usesKotlin ? "kotlin" : "java")
                .replaceAll("%REALM_VERSION%", "1.2.3.5")
                .replaceAll("%OS_TYPE%", System.getProperty("os.name"))
                .replaceAll("%OS_VERSION%", System.getProperty("os.version"));
    }

    /**
     * Computes an anonymous app/library id from the packages containing RealmObject classes
     * @return the anonymous app/library id
     * @throws NoSuchAlgorithmException
     */
    public String getAnonymousAppId() throws NoSuchAlgorithmException {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (String modelPackage : packages) {
//            stringBuilder.append(modelPackage).append(":");
//        }
//        byte[] packagesBytes = stringBuilder.toString().getBytes();

        return Utils.hexStringify(Utils.sha256Hash("Hello!".getBytes()));
    }
}
