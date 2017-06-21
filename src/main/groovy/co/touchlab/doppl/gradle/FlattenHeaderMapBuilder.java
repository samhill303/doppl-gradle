/*
 * Copyright (c) 2017 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.doppl.gradle;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kgalligan on 5/18/17.
 */
public class FlattenHeaderMapBuilder {
    public static void main(String[] args)
    {
        File file = new File(args[0]);

        makeMaps(System.out, new ArrayList<String>(), file);
    }

    private static void makeMaps(PrintStream writer, List<String> pathParts, File dir)
    {
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.getName().endsWith(".java");
            }
        });

        for (File file : files) {
            if(file.isDirectory())
            {
                List<String> asdf = new ArrayList<String>(pathParts);
                asdf.add(file.getName());
                makeMaps(writer, asdf, file);
            }
            else
            {
                String strippedName = file.getName().substring(0, file.getName().indexOf(".java"));
                for (String pathPart : pathParts) {
                    writer.append(pathPart).append('.');
                }
                writer.append(strippedName);
                writer.append('=');
                for (String pathPart : pathParts) {
                    writer.append(capitalize(pathPart));
                }
                writer.append(strippedName).append(".h\n");
            }
        }
    }

    private static String capitalize(String pathPart) {
        if(pathPart.length() == 0)
            return pathPart;
        if(pathPart.length() == 1)
            return pathPart.toUpperCase();
        else
            return pathPart.substring(0, 1).toUpperCase() + pathPart.substring(1);
    }
}
