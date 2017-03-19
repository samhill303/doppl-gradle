/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content;

import android.annotation.Nullable;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Interface to global information about an application environment.  This is
 * an abstract class whose implementation is provided by
 * the Android system.  It
 * allows access to application-specific resources and classes, as well as
 * up-calls for application-level operations such as launching activities,
 * broadcasting and receiving intents, etc.
 */
public abstract class Context {
    /**
     * File creation mode: the default mode, where the created file can only
     * be accessed by the calling application (or all applications sharing the
     * same user ID).
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     */
    public static final int MODE_PRIVATE = 0x0000;
    /**
     * @deprecated Creating world-readable files is very dangerous, and likely
     * to cause security holes in applications.  It is strongly discouraged;
     * instead, applications should use more formal mechanism for interactions
     * such as {@link ContentProvider}, {@link BroadcastReceiver}, and
     * {@link android.app.Service}.  There are no guarantees that this
     * access mode will remain on a file, such as when it goes through a
     * backup and restore.
     * File creation mode: allow all other applications to have read access
     * to the created file.
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_WRITEABLE
     */
    @Deprecated
    public static final int MODE_WORLD_READABLE = 0x0001;
    /**
     * @deprecated Creating world-writable files is very dangerous, and likely
     * to cause security holes in applications.  It is strongly discouraged;
     * instead, applications should use more formal mechanism for interactions
     * such as {@link ContentProvider}, {@link BroadcastReceiver}, and
     * {@link android.app.Service}.  There are no guarantees that this
     * access mode will remain on a file, such as when it goes through a
     * backup and restore.
     * File creation mode: allow all other applications to have write access
     * to the created file.
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     */
    @Deprecated
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    /**
     * File creation mode: for use with {@link #openFileOutput}, if the file
     * already exists then write data to the end of the existing file
     * instead of erasing it.
     * @see #openFileOutput
     */
    public static final int MODE_APPEND = 0x8000;


    /**
     * Database open flag: when set, the database is opened with write-ahead
     * logging enabled by default.
     *
     * @see #openOrCreateDatabase(String, int, CursorFactory)
     * @see #openOrCreateDatabase(String, int, CursorFactory, DatabaseErrorHandler)
     * @see SQLiteDatabase#enableWriteAheadLogging
     */
    public static final int MODE_ENABLE_WRITE_AHEAD_LOGGING = 0x0008;

    /**
     * Return the Looper for the main thread of the current process.  This is
     * the thread used to dispatch calls to application components (activities,
     * services, etc).
     * <p>
     * By definition, this method returns the same result as would be obtained
     * by calling {@link Looper#getMainLooper() Looper.getMainLooper()}.
     * </p>
     *
     * @return The main looper.
     */
    public abstract Looper getMainLooper();

    /**
     * {@hide}
     * Return the full path to the shared prefs file for the given prefs group name.
     *
     * <p>Note: this is not generally useful for applications, since they should
     * not be directly accessing the file system.
     */
    public abstract File getSharedPrefsFile(String name);

    /**
     * Retrieve and hold the contents of the preferences file 'name', returning
     * a SharedPreferences through which you can retrieve and modify its
     * values.  Only one instance of the SharedPreferences object is returned
     * to any callers for the same name, meaning they will see each other's
     * edits as soon as they are made.
     *
     * @param name Desired preferences file. If a preferences file by this name
     * does not exist, it will be created when you retrieve an
     * editor (SharedPreferences.edit()) and then commit changes (Editor.commit()).
     * @param mode Operating mode.  Use 0 or {@link #MODE_PRIVATE} for the
     * default operation, {@link #MODE_WORLD_READABLE}
     * and {@link #MODE_WORLD_WRITEABLE} to control permissions.
     *
     * @return The single {@link SharedPreferences} instance that can be used
     *         to retrieve and modify the preference values.
     *
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     */
    public abstract SharedPreferences getSharedPreferences(String name,
            int mode);

    /**
     * Open a private file associated with this Context's application package
     * for reading.
     *
     * @param name The name of the file to open; can not contain path
     *             separators.
     *
     * @return The resulting {@link FileInputStream}.
     *
     * @see #openFileOutput
     * @see #fileList
     * @see #deleteFile
     * @see java.io.FileInputStream#FileInputStream(String)
     */
    public abstract FileInputStream openFileInput(String name)
        throws FileNotFoundException;

    /**
     * Open a private file associated with this Context's application package
     * for writing. Creates the file if it doesn't already exist.
     * <p>
     * No additional permissions are required for the calling app to read or
     * write the returned file.
     *
     * @param name The name of the file to open; can not contain path
     *            separators.
     * @param mode Operating mode. Use 0 or {@link #MODE_PRIVATE} for the
     *            default operation, {@link #MODE_APPEND} to append to an
     *            existing file, {@link #MODE_WORLD_READABLE} and
     *            {@link #MODE_WORLD_WRITEABLE} to control permissions.
     * @return The resulting {@link FileOutputStream}.
     * @see #MODE_APPEND
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     * @see #openFileInput
     * @see #fileList
     * @see #deleteFile
     * @see java.io.FileOutputStream#FileOutputStream(String)
     */
    public abstract FileOutputStream openFileOutput(String name, int mode)
        throws FileNotFoundException;

    /**
     * Delete the given private file associated with this Context's
     * application package.
     *
     * @param name The name of the file to delete; can not contain path
     *             separators.
     *
     * @return {@code true} if the file was successfully deleted; else
     *         {@code false}.
     *
     * @see #openFileInput
     * @see #openFileOutput
     * @see #fileList
     * @see java.io.File#delete()
     */
    public abstract boolean deleteFile(String name);

    /**
     * Returns the absolute path on the filesystem where a file created with
     * {@link #openFileOutput} is stored.
     * <p>
     * The returned path may change over time if the calling app is moved to an
     * adopted storage device, so only relative paths should be persisted.
     *
     * @param name The name of the file for which you would like to get
     *          its path.
     *
     * @return An absolute path to the given file.
     *
     * @see #openFileOutput
     * @see #getFilesDir
     * @see #getDir
     */
    public abstract File getFileStreamPath(String name);

    /**
     * Returns the absolute path to the directory on the filesystem where files
     * created with {@link #openFileOutput} are stored.
     * <p>
     * The returned path may change over time if the calling app is moved to an
     * adopted storage device, so only relative paths should be persisted.
     * <p>
     * No additional permissions are required for the calling app to read or
     * write files under the returned path.
     *
     * @return The path of the directory holding application files.
     * @see #openFileOutput
     * @see #getFileStreamPath
     * @see #getDir
     */
    public abstract File getFilesDir();

    /**
     * Returns the absolute path to the directory on the primary shared/external
     * storage device where the application can place persistent files it owns.
     * These files are internal to the applications, and not typically visible
     * to the user as media.
     * <p>
     * This is like {@link #getFilesDir()} in that these files will be deleted
     * when the application is uninstalled, however there are some important
     * differences:
     * <ul>
     * <li>Shared storage may not always be available, since removable media can
     * be ejected by the user. Media state can be checked using
     * {@link Environment#getExternalStorageState(File)}.
     * <li>There is no security enforced with these files. For example, any
     * application holding
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} can write to
     * these files.
     * </ul>
     * <p>
     * If a shared storage device is emulated (as determined by
     * {@link Environment#isExternalStorageEmulated(File)}), it's contents are
     * backed by a private user data partition, which means there is little
     * benefit to storing data here instead of the private directories returned
     * by {@link #getFilesDir()}, etc.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#KITKAT}, no permissions
     * are required to read or write to the returned path; it's always
     * accessible to the calling app. This only applies to paths generated for
     * package name of the calling application. To access paths belonging to
     * other packages,
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} and/or
     * {@link android.Manifest.permission#READ_EXTERNAL_STORAGE} are required.
     * <p>
     * On devices with multiple users (as described by {@link UserManager}),
     * each user has their own isolated shared storage. Applications only have
     * access to the shared storage for the user they're running as.
     * <p>
     * The returned path may change over time if different shared storage media
     * is inserted, so only relative paths should be persisted.
     * <p>
     * Here is an example of typical code to manipulate a file in an
     * application's shared storage:
     * </p>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/content/ExternalStorage.java
     * private_file}
     * <p>
     * If you supply a non-null <var>type</var> to this function, the returned
     * file will be a path to a sub-directory of the given type. Though these
     * files are not automatically scanned by the media scanner, you can
     * explicitly add them to the media database with
     * {@link android.media.MediaScannerConnection#scanFile(Context, String[], String[], android.media.MediaScannerConnection.OnScanCompletedListener)
     * MediaScannerConnection.scanFile}. Note that this is not the same as
     * {@link android.os.Environment#getExternalStoragePublicDirectory
     * Environment.getExternalStoragePublicDirectory()}, which provides
     * directories of media shared by all applications. The directories returned
     * here are owned by the application, and their contents will be removed
     * when the application is uninstalled. Unlike
     * {@link android.os.Environment#getExternalStoragePublicDirectory
     * Environment.getExternalStoragePublicDirectory()}, the directory returned
     * here will be automatically created for you.
     * <p>
     * Here is an example of typical code to manipulate a picture in an
     * application's shared storage and add it to the media database:
     * </p>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/content/ExternalStorage.java
     * private_picture}
     *
     * @param type The type of files directory to return. May be {@code null}
     *            for the root of the files directory or one of the following
     *            constants for a subdirectory:
     *            {@link android.os.Environment#DIRECTORY_MUSIC},
     *            {@link android.os.Environment#DIRECTORY_PODCASTS},
     *            {@link android.os.Environment#DIRECTORY_RINGTONES},
     *            {@link android.os.Environment#DIRECTORY_ALARMS},
     *            {@link android.os.Environment#DIRECTORY_NOTIFICATIONS},
     *            {@link android.os.Environment#DIRECTORY_PICTURES}, or
     *            {@link android.os.Environment#DIRECTORY_MOVIES}.
     * @return the absolute path to application-specific directory. May return
     *         {@code null} if shared storage is not currently available.
     * @see #getFilesDir
     * @see #getExternalFilesDirs(String)
     * @see Environment#getExternalStorageState(File)
     * @see Environment#isExternalStorageEmulated(File)
     * @see Environment#isExternalStorageRemovable(File)
     */
    @Nullable
    public abstract File getExternalFilesDir(@Nullable String type);

    /**
     * Returns absolute paths to application-specific directories on all
     * shared/external storage devices where the application can place
     * persistent files it owns. These files are internal to the application,
     * and not typically visible to the user as media.
     * <p>
     * This is like {@link #getFilesDir()} in that these files will be deleted
     * when the application is uninstalled, however there are some important
     * differences:
     * <ul>
     * <li>Shared storage may not always be available, since removable media can
     * be ejected by the user. Media state can be checked using
     * {@link Environment#getExternalStorageState(File)}.
     * <li>There is no security enforced with these files. For example, any
     * application holding
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} can write to
     * these files.
     * </ul>
     * <p>
     * If a shared storage device is emulated (as determined by
     * {@link Environment#isExternalStorageEmulated(File)}), it's contents are
     * backed by a private user data partition, which means there is little
     * benefit to storing data here instead of the private directories returned
     * by {@link #getFilesDir()}, etc.
     * <p>
     * Shared storage devices returned here are considered a stable part of the
     * device, including physical media slots under a protective cover. The
     * returned paths do not include transient devices, such as USB flash drives
     * connected to handheld devices.
     * <p>
     * An application may store data on any or all of the returned devices. For
     * example, an app may choose to store large files on the device with the
     * most available space, as measured by {@link StatFs}.
     * <p>
     * No additional permissions are required for the calling app to read or
     * write files under the returned path. Write access outside of these paths
     * on secondary external storage devices is not available.
     * <p>
     * The returned path may change over time if different shared storage media
     * is inserted, so only relative paths should be persisted.
     *
     * @param type The type of files directory to return. May be {@code null}
     *            for the root of the files directory or one of the following
     *            constants for a subdirectory:
     *            {@link android.os.Environment#DIRECTORY_MUSIC},
     *            {@link android.os.Environment#DIRECTORY_PODCASTS},
     *            {@link android.os.Environment#DIRECTORY_RINGTONES},
     *            {@link android.os.Environment#DIRECTORY_ALARMS},
     *            {@link android.os.Environment#DIRECTORY_NOTIFICATIONS},
     *            {@link android.os.Environment#DIRECTORY_PICTURES}, or
     *            {@link android.os.Environment#DIRECTORY_MOVIES}.
     * @return the absolute paths to application-specific directories. Some
     *         individual paths may be {@code null} if that shared storage is
     *         not currently available. The first path returned is the same as
     *         {@link #getExternalFilesDir(String)}.
     * @see #getExternalFilesDir(String)
     * @see Environment#getExternalStorageState(File)
     * @see Environment#isExternalStorageEmulated(File)
     * @see Environment#isExternalStorageRemovable(File)
     */
    public abstract File[] getExternalFilesDirs(String type);

    /**
     * Returns the absolute path to the application specific cache directory on
     * the filesystem. These files will be ones that get deleted first when the
     * device runs low on storage. There is no guarantee when these files will
     * be deleted.
     * <p>
     * <strong>Note: you should not <em>rely</em> on the system deleting these
     * files for you; you should always have a reasonable maximum, such as 1 MB,
     * for the amount of space you consume with cache files, and prune those
     * files when exceeding that space.</strong>
     * <p>
     * The returned path may change over time if the calling app is moved to an
     * adopted storage device, so only relative paths should be persisted.
     * <p>
     * Apps require no extra permissions to read or write to the returned path,
     * since this path lives in their private storage.
     *
     * @return The path of the directory holding application cache files.
     * @see #openFileOutput
     * @see #getFileStreamPath
     * @see #getDir
     */
    public abstract File getCacheDir();

    /**
     * Returns an array of strings naming the private files associated with
     * this Context's application package.
     *
     * @return Array of strings naming the private files.
     *
     * @see #openFileInput
     * @see #openFileOutput
     * @see #deleteFile
     */
    public abstract String[] fileList();

    /**
     * Retrieve, creating if needed, a new directory in which the application
     * can place its own custom data files.  You can use the returned File
     * object to create and access files in this directory.  Note that files
     * created through a File object will only be accessible by your own
     * application; you can only set the mode of the entire directory, not
     * of individual files.
     * <p>
     * The returned path may change over time if the calling app is moved to an
     * adopted storage device, so only relative paths should be persisted.
     * <p>
     * Apps require no extra permissions to read or write to the returned path,
     * since this path lives in their private storage.
     *
     * @param name Name of the directory to retrieve.  This is a directory
     * that is created as part of your application data.
     * @param mode Operating mode.  Use 0 or {@link #MODE_PRIVATE} for the
     * default operation, {@link #MODE_WORLD_READABLE} and
     * {@link #MODE_WORLD_WRITEABLE} to control permissions.
     *
     * @return A {@link File} object for the requested directory.  The directory
     * will have been created if it does not already exist.
     *
     * @see #openFileOutput(String, int)
     */
    public abstract File getDir(String name, int mode);

    /**
     * Open a new private SQLiteDatabase associated with this Context's
     * application package.  Create the database file if it doesn't exist.
     *
     * @param name The name (unique in the application package) of the database.
     * @param mode Operating mode.  Use 0 or {@link #MODE_PRIVATE} for the
     *     default operation, {@link #MODE_WORLD_READABLE}
     *     and {@link #MODE_WORLD_WRITEABLE} to control permissions.
     *     Use {@link #MODE_ENABLE_WRITE_AHEAD_LOGGING} to enable write-ahead logging by default.
     * @param factory An optional factory class that is called to instantiate a
     *     cursor when query is called.
     *
     * @return The contents of a newly created database with the given name.
     * @throws android.database.sqlite.SQLiteException if the database file could not be opened.
     *
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     * @see #MODE_ENABLE_WRITE_AHEAD_LOGGING
     * @see #deleteDatabase
     */
    public abstract SQLiteDatabase openOrCreateDatabase(String name,
            int mode, CursorFactory factory);

    /**
     * Open a new private SQLiteDatabase associated with this Context's
     * application package.  Creates the database file if it doesn't exist.
     *
     * <p>Accepts input param: a concrete instance of {@link DatabaseErrorHandler} to be
     * used to handle corruption when sqlite reports database corruption.</p>
     *
     * @param name The name (unique in the application package) of the database.
     * @param mode Operating mode.  Use 0 or {@link #MODE_PRIVATE} for the
     *     default operation, {@link #MODE_WORLD_READABLE}
     *     and {@link #MODE_WORLD_WRITEABLE} to control permissions.
     *     Use {@link #MODE_ENABLE_WRITE_AHEAD_LOGGING} to enable write-ahead logging by default.
     * @param factory An optional factory class that is called to instantiate a
     *     cursor when query is called.
     * @param errorHandler the {@link DatabaseErrorHandler} to be used when sqlite reports database
     * corruption. if null, {@link android.database.DefaultDatabaseErrorHandler} is assumed.
     * @return The contents of a newly created database with the given name.
     * @throws android.database.sqlite.SQLiteException if the database file could not be opened.
     *
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     * @see #MODE_ENABLE_WRITE_AHEAD_LOGGING
     * @see #deleteDatabase
     */
    public abstract SQLiteDatabase openOrCreateDatabase(String name,
            int mode, CursorFactory factory,
            @Nullable DatabaseErrorHandler errorHandler);

    /**
     * Delete an existing private SQLiteDatabase associated with this Context's
     * application package.
     *
     * @param name The name (unique in the application package) of the
     *             database.
     *
     * @return {@code true} if the database was successfully deleted; else {@code false}.
     *
     * @see #openOrCreateDatabase
     */
    public abstract boolean deleteDatabase(String name);

    /**
     * Returns the absolute path on the filesystem where a database created with
     * {@link #openOrCreateDatabase} is stored.
     * <p>
     * The returned path may change over time if the calling app is moved to an
     * adopted storage device, so only relative paths should be persisted.
     *
     * @param name The name of the database for which you would like to get
     *          its path.
     *
     * @return An absolute path to the given database.
     *
     * @see #openOrCreateDatabase
     */
    public abstract File getDatabasePath(String name);

    /**
     * Returns an array of strings naming the private databases associated with
     * this Context's application package.
     *
     * @return Array of strings naming the private databases.
     *
     * @see #openOrCreateDatabase
     * @see #deleteDatabase
     */
    public abstract String[] databaseList();

    /**
     * Yeah, ugly, but this is all pretty simple right now.
     * @return
     */
    public Context getApplicationContext()
    {
        return this;
    }
}
