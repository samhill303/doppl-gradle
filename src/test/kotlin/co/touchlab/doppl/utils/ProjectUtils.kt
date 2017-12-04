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

package co.touchlab.doppl.utils

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

fun replaceFile(projectFile: File, path: String , contents: String){
    File(projectFile, path).writeText(contents)
}

fun validateFileContent(file: File, checker: (test: String) -> Boolean):Boolean {
    val reader = BufferedReader(FileReader(file))
    reader.use { reader ->
        val readText = reader.readText()
        return checker(readText)
    }
}

fun findObjcClassDefinition(headerFile: File, classDefinition: String):Boolean{
    return validateFileContent(headerFile, checker = {s ->
        s.contains("@protocol $classDefinition ") || s.contains("@interface $classDefinition ")
    })
}
