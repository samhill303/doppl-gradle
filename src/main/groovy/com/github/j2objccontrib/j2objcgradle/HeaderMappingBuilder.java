package com.github.j2objccontrib.j2objcgradle;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by kgalligan on 10/27/16.
 */
public class HeaderMappingBuilder {
    public static void main(String[] args)
    {
        File frameworksDistDir = new File(args[0]);
        File[] frameworkFiles = frameworksDistDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".framework");
            }
        });

        Properties properties = new Properties();

        for (File frameworkFile : frameworkFiles) {
            File headers = new File(frameworkFile, "Headers");
            String frameworkFileName = frameworkFile.getName();
            String frameworkName = frameworkFileName.substring(0, frameworkFileName.indexOf(".framework"));

            fillHeaderMap(frameworkName, headers, "", properties);
        }

        try {
            FileWriter propsOut = new FileWriter(new File(frameworksDistDir, "j2objc.mappings"));
            properties.store(propsOut, "j2objc Headers");
            propsOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fillHeaderMap(String frameworkName, File dir, String basePath, Properties properties)
    {
        String dots = basePath.replace('/', '.');

        File[] files = dir.listFiles();

        for (File file : files) {
            if(file.isDirectory())
            {
                fillHeaderMap(frameworkName, file, basePath + file.getName() +"/", properties);
            }
            else
            {
                String thisName = file.getName();
                int hIndex = thisName.indexOf(".h");
                if(hIndex > -1)
                {
                    if(basePath.equals("") && thisName.equals(frameworkName + ".h"))
                    {
                        continue;
                    }
                    String typeName = dots + thisName.substring(0, hIndex);
                    properties.setProperty(typeName, frameworkName + "/" + frameworkName +".h");
                }
            }
        }
    }
    /*
    public static void main(String[] args)
    {
        File frameworksDistDir = new File(args[0]);
        File[] frameworkFiles = frameworksDistDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".framework");
            }
        });

        Properties properties = new Properties();

        for (File frameworkFile : frameworkFiles) {
            File headers = new File(frameworkFile, "Headers");
            String frameworkFileName = frameworkFile.getName();
            String frameworkName = frameworkFileName.substring(0, frameworkFileName.indexOf(".framework"));

            fillHeaderMap(frameworkName, headers, "", properties);
        }

        try {
            FileWriter propsOut = new FileWriter(new File(frameworksDistDir, "j2objc.mappings"));
            properties.store(propsOut, "j2objc Headers");
            propsOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fillHeaderMap(String frameworkName, File dir, String basePath, Properties properties)
    {
        String dots = basePath.replace('/', '.');

        File[] files = dir.listFiles();

        for (File file : files) {
            if(file.isDirectory())
            {
                fillHeaderMap(frameworkName, file, basePath + file.getName() +"/", properties);
            }
            else
            {
                String thisName = file.getName();
                int hIndex = thisName.indexOf(".h");
                if(hIndex > -1)
                {
                    if (basePath.equals("") && thisName.equals(frameworkName + ".h")) {
                        continue;
                    }

                    if(basePath.startsWith("android/") || basePath.startsWith("com/android")) {
                        String typeName = dots + thisName.substring(0, hIndex);
                        String fullName = basePath + thisName;

                        properties.setProperty(typeName, camelize(fullName));
                    }
                }
            }
        }
    }

    private static String camelize(String s)
    {
        String[] parts = s.split("/");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }

        return sb.toString();
    }
     */
}
