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

package android.os;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/*-[
  #include <sys/stat.h>
  ]-*/

/**
 * Tools for managing files.  Not for public consumption.
 * @hide
 */
public class FileUtils
{
    public static final int S_IRWXU = 00700;
    public static final int S_IRUSR = 00400;
    public static final int S_IWUSR = 00200;
    public static final int S_IXUSR = 00100;

    public static final int S_IRWXG = 00070;
    public static final int S_IRGRP = 00040;
    public static final int S_IWGRP = 00020;
    public static final int S_IXGRP = 00010;

    public static final int S_IRWXO = 00007;
    public static final int S_IROTH = 00004;
    public static final int S_IWOTH = 00002;
    public static final int S_IXOTH = 00001;
    
    
    /**
     * File status information. This class maps directly to the POSIX stat structure.
     * @hide
     */
    public static final class FileStatus {
        public int dev;
        public int ino;
        public int mode;
        public int nlink;
        public int uid;
        public int gid;
        public int rdev;
        public long size;
        public int blksize;
        public long blocks;
        public long atime;
        public long mtime;
        public long ctime;
    }
    
    /**
     * Get the status for the given path. This is equivalent to the POSIX stat(2) system call. 
     * @param path The path of the file to be stat'd.
     * @param status Optional argument to fill in. It will only fill in the status if the file
     * exists. 
     * @return true if the file exists and false if it does not exist. If you do not have 
     * permission to stat the file, then this method will return false.
     */
    public static native boolean getFileStatus(String path, FileStatus status)/*-[
//    const char* pathStr = env->GetStringUTFChars(path, NULL);
    jboolean ret = false;

    struct stat s;
    int res = stat([path cStringUsingEncoding: NSASCIIStringEncoding], &s);
    if (res == 0) {
        ret = true;
        if (status != NULL) {

            status->dev_ = s.st_dev;
            status->ino_ = s.st_ino;
            status->mode_ = s.st_mode;
            status->nlink_ = s.st_nlink;
            status->uid_ = s.st_uid;
            status->gid_ = s.st_gid;
            status->size_ = s.st_size;
            status->blksize_ = s.st_blksize;
            status->blocks_ = s.st_blocks;

            status->atime_ = s.st_atime;
            status->mtime_ = s.st_mtime;
            status->ctime_ = s.st_ctime;

//            env->SetIntField(fileStatus, gFileStatusDevFieldID, s.st_dev);
//            env->SetIntField(fileStatus, gFileStatusInoFieldID, s.st_ino);
//            env->SetIntField(fileStatus, gFileStatusModeFieldID, s.st_mode);
//            env->SetIntField(fileStatus, gFileStatusNlinkFieldID, s.st_nlink);
//            env->SetIntField(fileStatus, gFileStatusUidFieldID, s.st_uid);
//            env->SetIntField(fileStatus, gFileStatusGidFieldID, s.st_gid);
//            env->SetLongField(fileStatus, gFileStatusSizeFieldID, s.st_size);
//            env->SetIntField(fileStatus, gFileStatusBlksizeFieldID, s.st_blksize);
//            env->SetLongField(fileStatus, gFileStatusBlocksFieldID, s.st_blocks);
//            env->SetLongField(fileStatus, gFileStatusAtimeFieldID, s.st_atime);
//            env->SetLongField(fileStatus, gFileStatusMtimeFieldID, s.st_mtime);
//            env->SetLongField(fileStatus, gFileStatusCtimeFieldID, s.st_ctime);
        }
    }

//    env->ReleaseStringUTFChars(path, pathStr);

    return ret;
    ]-*/;

    /** Regular expression for safe filenames: no spaces or metacharacters */
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[\\w%+,./=_-]+");

    public static native int setPermissions(String file, int mode, int uid, int gid)/*-[
    if ([file length] <= 0) {
        return ENOENT;
    }
    if (uid >= 0 || gid >= 0) {
        int res = chown([file cStringUsingEncoding: NSASCIIStringEncoding], uid, gid);
        if (res != 0) {
            return errno;
        }
    }
    return chmod([file cStringUsingEncoding: NSASCIIStringEncoding], mode) == 0 ? 0 : errno;
    ]-*/;

    /** returns the FAT file system volume ID for the volume mounted
     * at the given mount point, or -1 for failure
     * @param mount point for FAT volume
     * @return volume ID or -1
     */
//    public static native int getFatVolumeId(String mountPoint);

    /**
     * Perform an fsync on the given FileOutputStream.  The stream at this
     * point must be flushed but not yet closed.
     */
    public static boolean sync(FileOutputStream stream) {
        try {
            if (stream != null) {
                stream.getFD().sync();
            }
            return true;
        } catch (IOException e) {
        }
        return false;
    }

    // copy a file from srcFile to destFile, return true if succeed, return
    // false if fail
    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = copyToFile(in, destFile);
            } finally  {
                in.close();
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }
    
    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if a filename is "safe" (no metacharacters or spaces).
     * @param file  The file to check
     */
    public static boolean isFilenameSafe(File file) {
        // Note, we check whether it matches what's known to be safe,
        // rather than what's known to be unsafe.  Non-ASCII, control
        // characters, etc. are all unsafe by default.
        return SAFE_FILENAME_PATTERN.matcher(file.getPath()).matches();
    }

    /**
     * Read a text file into a String, optionally limiting the length.
     * @param file to read (will not seek, so things like /proc files are OK)
     * @param max length (positive for head, negative of tail, 0 for no limit)
     * @param ellipsis to add of the file was truncated (can be null)
     * @return the contents of the file, possibly truncated
     * @throws IOException if something goes wrong reading the file
     */
    public static String readTextFile(File file, int max, String ellipsis) throws IOException {
        InputStream input = new FileInputStream(file);
        try {
            long size = file.length();
            if (max > 0 || (size > 0 && max == 0)) {  // "head" mode: read the first N bytes
                if (size > 0 && (max == 0 || size < max)) max = (int) size;
                byte[] data = new byte[max + 1];
                int length = input.read(data);
                if (length <= 0) return "";
                if (length <= max) return new String(data, 0, length);
                if (ellipsis == null) return new String(data, 0, max);
                return new String(data, 0, max) + ellipsis;
            } else if (max < 0) {  // "tail" mode: keep the last N
                int len;
                boolean rolled = false;
                byte[] last = null, data = null;
                do {
                    if (last != null) rolled = true;
                    byte[] tmp = last; last = data; data = tmp;
                    if (data == null) data = new byte[-max];
                    len = input.read(data);
                } while (len == data.length);

                if (last == null && len <= 0) return "";
                if (last == null) return new String(data, 0, len);
                if (len > 0) {
                    rolled = true;
                    System.arraycopy(last, len, last, 0, last.length - len);
                    System.arraycopy(data, 0, last, last.length - len, len);
                }
                if (ellipsis == null || !rolled) return new String(last);
                return ellipsis + new String(last);
            } else {  // "cat" mode: size unknown, read it all in streaming fashion
                ByteArrayOutputStream contents = new ByteArrayOutputStream();
                int len;
                byte[] data = new byte[1024];
                do {
                    len = input.read(data);
                    if (len > 0) contents.write(data, 0, len);
                } while (len == data.length);
                return contents.toString();
            }
        } finally {
            input.close();
        }
    }
}
