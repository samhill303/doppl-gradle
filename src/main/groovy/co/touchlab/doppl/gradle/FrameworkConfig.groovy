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

package co.touchlab.doppl.gradle

import co.touchlab.doppl.gradle.tasks.FrameworkTask
import co.touchlab.doppl.gradle.tasks.TranslateDependenciesTask
import co.touchlab.doppl.gradle.tasks.TranslateTask
import co.touchlab.doppl.gradle.tasks.Utils
import org.bouncycastle.asn1.cmp.PKIFreeText
import org.gradle.api.Project


class FrameworkConfig {
    private static final String SOURCE_EXTENSIONS = "h,m,cpp,properites,txt"

    static FrameworkConfig findMain(Project project) {
        return project.dopplConfig.mainFramework
    }

    static FrameworkConfig findTest(Project project) {
        return project.dopplConfig.testFramework
    }

    String homepage = "http://doppl.co/"
    String license = "{ :type => 'Apache 2.0' }"
    String authors = "{ 'Filler Person' => 'filler@example.com' }"
    String source = "{ :git => 'https://github.com/doppllib/doppl-gradle.git'}"

    final boolean test

    FrameworkConfig(boolean test) {
        this.test = test
    }

    boolean writeActualJ2objcPath = true

    String iosDeploymentTarget = "8.0"

    boolean flagObjc = true
    boolean libZ = true
    boolean libSqlite3 = true
    boolean libIconv = true
    boolean libJavax_inject = true
    boolean libJre_emul = true
    boolean libJsr305 = true
    boolean libGuava = test
    boolean libMockito = false
    boolean libJunit = false

    List<String> managedPodsList = new ArrayList<>()

    void managePod(String... paths)
    {
        for (String p : paths) {
            managedPodsList.add(p)
        }
    }

    List<String> addLibraries = new ArrayList<>()

    void addLibraries(String... libs)
    {
        for (String l : libs) {
            this.addLibraries.add(l)
        }
    }

    boolean frameworkUIKit = true

    List<String> addFrameworks = new ArrayList<>()

    void addFrameworks(String... frameworks)
    {
        for (String f : frameworks) {
            this.addFrameworks(f)
        }
    }

    String writeLibs()
    {
        List<String> allLibs = new ArrayList<>(addLibraries)
        if(libZ)allLibs.add("z")
        if(libSqlite3)allLibs.add("sqlite3")
        if(libIconv)allLibs.add("iconv")
        if(libJavax_inject)allLibs.add("javax_inject")
        if(libJre_emul)allLibs.add("jre_emul")
        if(libJsr305)allLibs.add("jsr305")
        if(libGuava)allLibs.add("guava")
        if(libMockito)allLibs.add("mockito")
        if(libJunit)allLibs.add("junit")

        return "'"+ allLibs.join("', '") +"'"
    }

    String writeFrameworks()
    {
        List<String> allFrameworks = new ArrayList<>(addFrameworks)
        if(frameworkUIKit)allFrameworks.add("UIKit")

        return "'"+ allFrameworks.join("', '") +"'"
    }

    String makePodFileList(List<String> parts)
    {
        StringBuilder sourceFileIncludes = new StringBuilder()
        for (String folderName : parts) {
            if(sourceFileIncludes.length() == 0)
            {
                sourceFileIncludes.append("FileList[\"${folderName}\"]")
            }
            else {
                sourceFileIncludes.append(".include(\"${folderName}\")")
            }
        }

        return sourceFileIncludes.toString()
    }

    String relativeOrNull(File parent, File target, boolean failOnAbsolute) {
        String path = Utils.relativePath(parent, target)
        if (path.startsWith("/"))
        {
            if(failOnAbsolute)
                throw new IllegalArgumentException("Absolute path for ${target.getPath()}")
            else
                return null
        }
        else {
            return path
        }
    }

    String podspecTemplate (
            Project project,
            File globalHeaderFile,
            List<File> objcFolders,
            List<File> headerFolders,
            List<File> javaFolders,
            String podname){

        List<String> sourceLines = new ArrayList<>()
        List<String> headerLines = new ArrayList<>()

        String allHeadersInclude = relativeOrNull(project.projectDir, globalHeaderFile, true)

        sourceLines.add("${allHeadersInclude}")
        headerLines.add("${allHeadersInclude}")

        //Get loose Objc and c/c++ source into project
        for (File folder : objcFolders) {
            sourceLines.add("${relativeOrNull(project.projectDir, folder, true)}/**/*.{${SOURCE_EXTENSIONS}}")
        }

        for (File folder : headerFolders) {
            headerLines.add("${relativeOrNull(project.projectDir, folder, true)}/**/*.h")
        }

        for (File folder : javaFolders) {
            String javaPath = relativeOrNull(project.projectDir, folder, false)
            if(javaPath != null) {
                sourceLines.add("${javaPath}/**/*.java")
            }
        }

        String sourceFiles = makePodFileList(sourceLines)

        String headers = makePodFileList(headerLines)

        String objcFlagString = flagObjc ? ",\n     'OTHER_LDFLAGS' => '-ObjC'" : ""

        String j2objcPath = writeActualJ2objcPath ? Utils.j2objcHome(project) : "\$(J2OBJC_LOCAL_PATH)"
        return """require 'rake'
FileList = Rake::FileList

Pod::Spec.new do |s|

  s.name             = '${podname}'
    s.version          = '0.1.0'
    s.summary          = 'Doppl code framework'

    s.description      = <<-DESC
  TODO: Add long description of the pod here.
                         DESC

    s.homepage         = '${homepage}'
    s.license          = ${license}
    s.authors           = ${authors}
    s.source           = ${source}

    s.ios.deployment_target = '${iosDeploymentTarget}'

    s.source_files = ${sourceFiles}.to_ary

    s.public_header_files = ${headers}.exclude(/cpphelp/).exclude(/jni/).to_ary

    s.requires_arc = false
    s.libraries = ${writeLibs()}
    s.frameworks = ${writeFrameworks()}

    s.pod_target_xcconfig = {
     'HEADER_SEARCH_PATHS' => '${j2objcPath}/include','LIBRARY_SEARCH_PATHS' => '${j2objcPath}/lib'${objcFlagString}
    }
    
    s.user_target_xcconfig = {
     'HEADER_SEARCH_PATHS' => '${j2objcPath}/frameworks/JRE.framework/Headers ${j2objcPath}/frameworks/JavaxInject.framework/Headers ${j2objcPath}/frameworks/JSR305.framework/Headers ${j2objcPath}/frameworks/JUnit.framework/Headers ${j2objcPath}/frameworks/Mockito.framework/Headers ${j2objcPath}/frameworks/Xalan.framework/Headers ${j2objcPath}/frameworks/Guava.framework/Headers'
    }
    
    
    
end"""}
}
