package com.github.j2objccontrib.j2objcgradle.tasks

/**
 * Created by kgalligan on 3/28/16.
 */
class FixLibraryPaths {
    public static void main(String[] args)
    {

        def rootPath = new File(args[0])

        def transformDirs = rootPath.listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                return pathname.isDirectory()
            }
        })

        List<File> allFiles = new ArrayList<>()

        renameRecursive(rootPath, rootPath, allFiles)

        TranslateTask.remapHeaderLinks(allFiles.toArray(new File[allFiles.size()]), rootPath, null)

        /*def headerFixList = rootPath.listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                return !pathname.isDirectory() && (pathname.getName().endsWith(".h") || pathname.getName().endsWith(".m"))
            }
        })

        headerFixList.each {File f ->
            TranslateTask.modifyIncludeForFile(f, null)
        }*/
    }

    static void renameRecursive(File rootDir, File dir, List<File> allFiles)
    {
        def files = dir.listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                return pathname.isDirectory() || (pathname.getName().endsWith(".h") || pathname.getName().endsWith(".m"))
            }
        })

        files.each {File f ->
            if(f.isDirectory())
            {
                renameRecursive(rootDir, f, allFiles)
            }
            else
            {
                allFiles.add(f)
                println "Adding: "+ f.getPath()
                /*def relativePath = f.getPath().substring(rootDir.getPath().length())

                def renamePath = TranslateTask.findTransformedFilePath(relativePath, null)
                if(renamePath != null && renamePath.length() > 0)
                {
                    f.renameTo(new File(rootDir, renamePath))
                }*/
            }
        }


    }
}
