/*
 *  Copyright (C) 2016-present Albie Liang. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package cc.suitalk.gradle.plugin

import cc.suitalk.arbitrarygen.core.AGContextExtensionManager
import cc.suitalk.arbitrarygen.core.ArgsConstants
import cc.suitalk.arbitrarygen.core.Core
import cc.suitalk.moduleapi.ag.extension.ModuleApiAGContextExtension
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * Created by albieliang on 2018/2/1.
 *
 */
class ModuleApiAGExtensionPlugin implements Plugin<Project> {

    Project project;

    @Override
    void apply(Project project) {
        this.project = project
        // Add AGContextExtension
        AGContextExtensionManager.getImpl().addExtension(ModuleApiAGContextExtension.class)
        println("project ${project.name} apply ModuleApiAGExtensionPlugin")

        project.afterEvaluate {
            def arbitraryGen = this.project["arbitraryGen"]
            if (arbitraryGen == null) {
                println("closure arbitraryGen is null, skip")
                return
            }
            if (arbitraryGen.ext == null) {
                println("arbitraryGen.ext closure is null, skip.")
                return
            }

            JsonBuilder builder = new JsonBuilder()
            builder {
                javaCodeEngine (arbitraryGen.javaCodeEngine == null ? {} : arbitraryGen.javaCodeEngine)
                ext (arbitraryGen.ext == null ? {} : arbitraryGen.ext)
            }

            println("closure : ${builder.toPrettyString()}")
            def agClosure = (new JsonSlurper()).parseText(builder.toString())
//            println("${ext}")
            def moduleApi = agClosure.ext.moduleApi
            if (moduleApi == null) {
                println("moduleApi closure is null, skip.")
                return
            }
            println "moduleApi(${this.project.name}) srcDir : '${moduleApi.srcDir}'"
            println "moduleApi(${this.project.name}) destDir : '${moduleApi.destDir}'"

            File srcDir = this.project.file(moduleApi.srcDir)
            File destDir = this.project.file(moduleApi.destDir)

            if (srcDir == null || !srcDir.exists() || destDir == null || !destDir.exists()) {
                println("project: ${this.project} do not exists moduleApi srcDir or destDir.")
                return
            }
            copyFiles(srcDir, destDir)
        }
    }

    def copyFiles(JSONArray srcRules, File destDir) {
        println("copyFiles($srcRules, $destDir)")
        List<File> list = collectFiles(srcRules)
        for (File file : list) {
            String path = file.getAbsolutePath()
            String destPath = path.replaceFirst(srcDir.getAbsolutePath(), destDir.getAbsolutePath())
                    .replaceAll('^([^\\.]*)\\.api$', '$1.java')
            copyFile(file, new File(destPath))
            println("copy file($path) to $destPath")
        }
    }

    List<File> collectFiles(JSONArray srcRules) {
        List<File> list = new LinkedList();
        JSONObject args = new JSONObject();
        args.put(ArgsConstants.EXTERNAL_ARGS_KEY_RULE, srcRules)
        JSONObject result = Core.exec("rule-processor", args)
        JSONArray fileArray = result.getJSONArray("fileArray")
        if (fileArray.isEmpty()) {
            return list
        }
        for (int i = 0; i < fileArray.size(); i++) {
            String path = fileArray.getString(i)
            if (path == null) {
                continue
            }
            if (path.endsWith(".api")) {
                list.add(new File(path))
            }
        }
        return list
    }

    def copyFiles(File srcDir, File destDir) {
        println("copyFiles($srcDir, $destDir)")
        List<File> list = new LinkedList();
        collectFiles(list, srcDir)
        for (File file : list) {
            String path = file.getAbsolutePath()
            String destPath = path.replaceFirst(srcDir.getAbsolutePath(), destDir.getAbsolutePath())
                    .replaceAll('^([^\\.]*)\\.api$', '$1.java')
            copyFile(file, new File(destPath))
            println("copy file($path) to $destPath")
        }
    }

    def copyFile(File srcFile, File destFile) {
        InputStream is = null
        OutputStream os = null
        try {
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs()
            }
            is = new FileInputStream(srcFile)
            os = new FileOutputStream(destFile)
            final int bufferSize = 1024 * 64
            byte[] buffer = new byte[bufferSize]
            int ret = 0;
            while ((ret = is.read(buffer)) != -1) {
                os.write(buffer, 0, ret)
            }
            os.flush()
            return
        } catch (Exception e) {
            println("copyFile error : $e")
        } finally {
            if (is != null) {
                try {
                    is.close()
                } catch (Exception e) {
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (Exception e) {
                }
            }
        }
    }

    def collectFiles(List<File> list, File file) {
        if (file.isFile()) {
            if (file.name.endsWith(".api")) {
                list.add(file)
            }
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                collectFiles(list, f)
            }
        }
    }
}

