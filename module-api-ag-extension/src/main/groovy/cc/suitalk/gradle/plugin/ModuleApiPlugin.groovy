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

import cc.suitalk.arbitrarygen.core.ArbitraryGenContext
import cc.suitalk.arbitrarygen.core.ArgsConstants
import cc.suitalk.arbitrarygen.core.Core
import cc.suitalk.arbitrarygen.core.DefaultArbitraryGenInitializer
import cc.suitalk.arbitrarygen.engine.JavaCodeAGEngine
import cc.suitalk.arbitrarygen.extension.AGContext
import cc.suitalk.arbitrarygen.extension.ArbitraryGenProcessor
import cc.suitalk.arbitrarygen.impl.AGAnnotationWrapper
import cc.suitalk.arbitrarygen.template.TemplateManager
import cc.suitalk.arbitrarygen.tools.RuntimeContextHelper
import cc.suitalk.arbitrarygen.utils.FileOperation
import cc.suitalk.arbitrarygen.utils.JSONArgsUtils
import cc.suitalk.arbitrarygen.utils.Log
import cc.suitalk.arbitrarygen.utils.Util
import cc.suitalk.moduleapi.ag.extension.ModuleApiAGContextExtension
import cc.suitalk.moduleapi.ag.extension.ModuleApiTaskProcessor
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * Created by albieliang on 2018/2/1.
 *
 */
class ModuleApiPlugin implements Plugin<Project> {

    static {
        loadTemplate("module-api-gentask", "/res/ag-template/ModuleApi.ag-template");
    }

    private static void loadTemplate(String tag, String templatePath) {
        String template = "";
        InputStream is = ModuleApiAGContextExtension.class.getResourceAsStream(templatePath);
//        Log.i(TAG, "doGet(jarPath : %s)", ModuleApiAGContextExtension.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        if (is != null) {
            template = FileOperation.read(is);
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        if (template != null && template.length() > 0) {
            TemplateManager.getImpl().put(tag, template);
        }
    }

    Project project;
    ModuleApiPluginExtension extension;

    @Override
    void apply(Project project) {
        this.project = project
        println("project ${project.name} apply ModuleApiPlugin")

        extension = project.extensions.create("moduleApi", ModuleApiPluginExtension)
        project.afterEvaluate {
            JSONObject initArg = getArgsFromExtension(this.project)
            if (initArg == null) {
                initArg = getArgsFromConfigFile(this.project)
            }
            if (initArg == null) {
                initArg = new JSONObject();
                initArg.put("srcDir", "${this.project.projectDir.absolutePath}/src/main/api".toString())
                initArg.put("destDir", "${this.project.rootProject.projectDir.absolutePath}/api/src/main/api".toString())
            }
            println "moduleApi(${this.project.name}) srcDir : '${initArg.getString("srcDir")}'"
            println "moduleApi(${this.project.name}) destDir : '${initArg.getString("destDir")}'"
            println "moduleApi(${this.project.name}) makeApiRules : '${initArg.get("makeApiRules")}'"

            File srcDir = this.project.file(initArg.getString("srcDir"))
            File destDir = this.project.file(initArg.getString("destDir"))

            if (srcDir == null || !srcDir.exists() || destDir == null) {
                println("project: ${this.project} do not exists moduleApi srcDir or destDir.")
                return
            }
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            // copy and rename `*.api` files
            copyFiles(srcDir, destDir)

            JSONObject envJson = new JSONObject();

            envJson.put("buildDir", this.project.buildDir.getAbsolutePath())
            JSONObject projectJson = new JSONObject();
            projectJson.put("name", this.project.name)
            projectJson.put("projectDir", this.project.projectDir.getAbsolutePath())
            projectJson.put("rootDir", this.project.rootDir.getAbsolutePath())
            envJson.put("project", projectJson)

            makeApi(initArg, envJson)
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
            String destPath = (destDir.getAbsolutePath() + path.substring(srcDir.getAbsolutePath().length()))
            if (destPath.endsWith(".api")) {
                destPath = destPath.substring(0, destPath.length() - ".api".length()) + ".java"
            }
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

    def makeApi(JSONObject initArgs, JSONObject envJson) {
        JSONArray makeApiRules = JSONArgsUtils.getJSONArray(initArgs, "makeApiRules", true);
        if (makeApiRules == null) {
            makeApiRules = new JSONArray();
        }
        if (makeApiRules.isEmpty()) {
            String srcDir = initArgs.getString("srcDir");
            if (srcDir == null || srcDir.length() == 0) {
                Log.e(TAG, "makeApi, neither moduleApi.srcDir nor moduleApi.makeApiRules is null or nil, skip.");
                return;
            }
            if (srcDir.endsWith("/")) {
                makeApiRules.add(srcDir + "*");
            } else {
                makeApiRules.add(srcDir + "/*");
            }
        }
        String destDir = initArgs.getString("destDir");
        if (destDir == null || destDir.length() == 0) {
            Log.e(TAG, "makeApi, moduleApi.destDir is null or nil, skip.");
            return;
        }

        println("${initArgs.toString()}")
        println("${envJson.toString()}")

        Core.initialize(DefaultArbitraryGenInitializer.INSTANCE);
        //
        // Initialize Environment arguments
        RuntimeContextHelper.initialize(envJson);
        // For new engine framework
        AGContext context = new ArbitraryGenContext();

        ArbitraryGenProcessor processor = context.getProcessor("javaCodeEngine");
        if (processor instanceof JavaCodeAGEngine) {
            JavaCodeAGEngine engine = (JavaCodeAGEngine) processor;
            AGAnnotationWrapper annWrapper = new AGAnnotationWrapper();
            annWrapper.addAnnotationProcessor(new ModuleApiTaskProcessor());
            engine.addTypeDefWrapper(annWrapper);

            JSONObject javaCodeEngine = new JSONObject();
            JSONArray ruleArray = new JSONArray();
            ruleArray.addAll(makeApiRules);
            javaCodeEngine.put("rule", ruleArray);
            initArgs.put("javaCodeEngine", javaCodeEngine);
            context.getKeyValueSet().put("apiDestPath", destDir);
        }
        context.initialize(initArgs);
        context.execute();

        Log.close();
    }

    JSONObject getArgsFromConfigFile(Project project) {
        String shellCode = FileOperation.read("${project.rootDir.absolutePath}/.config/module-api.ag-conf")
        if (shellCode == null || shellCode.length() == 0) {
            println("shellCode is null or nil, config file path : ${project.rootDir.absolutePath}/.config/module-api.ag-conf")
            return null
        }
        Binding binding = new Binding()
        binding.setProperty("project", project)
        binding.setProperty("name", project.name)
        binding.setProperty("rootDir", project.rootDir)
        binding.setProperty("projectDir", project.projectDir.absolutePath)
        binding.setProperty("buildDir", project.buildDir.absolutePath)

        GroovyShell groovyShell = new GroovyShell(binding)
        StringBuilder builder = new StringBuilder()
        builder.append("import groovy.json.JsonBuilder\n")
        builder.append("JsonBuilder moduleApi = new JsonBuilder()\n")
        builder.append(shellCode)
        builder.append("\n")
        builder.append("moduleApi.toString()")
        Object result = groovyShell.evaluate(builder.toString())
        return JSONObject.fromObject(result.toString())
    }

    JSONObject getArgsFromExtension(Project project) {
        def moduleApiClosure = project["moduleApi"]
        if (moduleApiClosure == null) {
            println("getArgsFromExtension, closure moduleApi is null, skip")
            return null
        }
        if (moduleApiClosure.srcDir == null || moduleApiClosure.destDir == null) {
            println("getArgsFromExtension, project(${project.name}), moduleApi.srcDir or moduleApi.destDir is null")
            return null
        }
        JSONObject initArg = new JSONObject();
        initArg.put("srcDir", (moduleApiClosure.srcDir == null ? "" : moduleApiClosure.srcDir))
        initArg.put("destDir", (moduleApiClosure.destDir == null ? "" : moduleApiClosure.destDir))
        initArg.put("templateDir", (moduleApiClosure.templateDir == null ? "" : moduleApiClosure.templateDir))
        if (moduleApiClosure.makeApiRules != null && moduleApiClosure.makeApiRules.length > 0) {
            JSONArray jsonArray = new JSONArray();
            for (String rule : moduleApiClosure.makeApiRules) {
                jsonArray.add(rule)
            }
            initArg.put("makeApiRules", jsonArray)
        }
        if (moduleApiClosure.logger != null) {
            JsonBuilder jsonBuilder = new JsonBuilder()
            jsonBuilder {
                logger moduleApiClosure.logger
            }
            println("ModuleApi : ${jsonBuilder.toString()}")
            JSONObject loggerJson = JSONObject.fromObject(jsonBuilder.toString())
            if (!loggerJson.isNullObject() && !loggerJson.isEmpty()) {
                initArg.put("logger", loggerJson.get("logger"))
            }

        }
        return initArg
    }
}

