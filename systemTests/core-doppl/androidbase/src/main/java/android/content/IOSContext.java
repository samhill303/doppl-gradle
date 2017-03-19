package android.content;
import android.annotation.Nullable;
import android.app.Application;
import android.app.SharedPreferencesImpl;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.os.FileUtils.FileStatus;

import android.internal.util.XmlUtils;


/**
 * Created by kgalligan on 3/21/16.
 */
public class IOSContext extends Application
{
    public static final String DATABASES = "databases";
    public static final String TAG = "IOSContext";


    private final Object mSync = new Object();
    private File mPreferencesDir;

    private final HashMap<String, SharedPreferencesImpl> sSharedPrefs = new HashMap<String, SharedPreferencesImpl>();

    @Override
    public Looper getMainLooper()
    {
        return Looper.getMainLooper();
    }

    private static File makeBackupFile(File prefsFile)
    {
        return new File(prefsFile.getPath() + ".bak");
    }

    @Override
    public File getSharedPrefsFile(String name)
    {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }

    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
                "File " + name + " contains a path separator");
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode)
    {
        SharedPreferencesImpl sp;
        File prefsFile;
        boolean needInitialLoad = false;
        synchronized(sSharedPrefs) {
            sp = sSharedPrefs.get(name);
            if (sp != null && !sp.hasFileChangedUnexpectedly()) {
                return sp;
            }
            prefsFile = getSharedPrefsFile(name);
            if (sp == null) {
                sp = new SharedPreferencesImpl(prefsFile, mode, null, new Handler(Looper.getMainLooper()));
                sSharedPrefs.put(name, sp);
                needInitialLoad = true;
            }
        }

        synchronized (sp) {
            if (needInitialLoad && sp.isLoaded()) {
                // lost the race to load; another thread handled it
                return sp;
            }
            File backup = makeBackupFile(prefsFile);
            if (backup.exists()) {
                prefsFile.delete();
                backup.renameTo(prefsFile);
            }

            // Debugging
            if (prefsFile.exists() && !prefsFile.canRead()) {
                Log.w(TAG, "Attempt to read preferences file " + prefsFile + " without permission");
            }

            Map map = null;
            FileUtils.FileStatus stat = new FileStatus();
            if (FileUtils.getFileStatus(prefsFile.getPath(), stat) && prefsFile.canRead()) {
                try {
                    FileInputStream str = new FileInputStream(prefsFile);
                    map = XmlUtils.readMapXml(str);
                    str.close();
                } catch (org.xmlpull.v1.XmlPullParserException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                } catch (IOException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                }
            }
            sp.replace(map, stat);
        }
        return sp;
    }

    private File getPreferencesDir() {
        synchronized (mSync) {
            if (mPreferencesDir == null) {
                mPreferencesDir = getDir("shared_prefs", MODE_PRIVATE);
            }
            return mPreferencesDir;
        }
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException
    {
        return new FileInputStream(findLocalFile(name));
    }

    /**
     * TODO: DO SOMETHING WITH MODE
     * @param name The name of the file to open; can not contain path
     *            separators.
     * @param mode Operating mode. Use 0 or {@link #MODE_PRIVATE} for the
     *            default operation, {@link #MODE_APPEND} to append to an
     *            existing file, {@link #MODE_WORLD_READABLE} and
     *            {@link #MODE_WORLD_WRITEABLE} to control permissions.
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException
    {
        return new FileOutputStream(findLocalFile(name));
    }

    private File findLocalFile(String name)
    {
        return new File(getFilesDir(), name);
    }

    @Override
    public boolean deleteFile(String name)
    {
        final File localFile = findLocalFile(name);
        return localFile.delete();
    }

    @Override
    public File getFileStreamPath(String name)
    {
        return findLocalFile(name);
    }

    @Override
    public File getFilesDir()
    {
        return getDir("files", MODE_PRIVATE);
    }

    protected File getRootDir()
    {
        final File file = new File(getRootDirString());
        file.mkdirs();
        return file;
    }

    private native String getRootDirString()/*-[
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);

    NSString *documentsDirectory = [paths firstObject];

return documentsDirectory;
]-*/;

    @Override
    public File getExternalFilesDir(@Nullable String type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File[] getExternalFilesDirs(String type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getCacheDir()
    {
        return getDir("cache", MODE_PRIVATE);
    }

    @Override
    public String[] fileList()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDir(String name, int mode)
    {
        final File rootDir = getRootDir();
        final File filesDir = new File(rootDir, name);
        filesDir.mkdirs();
        return filesDir;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory)
    {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler)
    {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(), factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name)
    {
        return getDatabasePath(name).delete();
    }

    @Override
    public File getDatabasePath(String name)
    {
        final File dbDir = getDir(DATABASES, MODE_PRIVATE);
        final File file = new File(dbDir, name);
        return file;
    }

    @Override
    public String[] databaseList()
    {
        return getDir(DATABASES, MODE_PRIVATE).list();
    }
}
