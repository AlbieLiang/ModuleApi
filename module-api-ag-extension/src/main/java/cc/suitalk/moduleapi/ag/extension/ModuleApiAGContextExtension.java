/*
 *  Copyright (C) 2017-present Albie Liang. All rights reserved.
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

package cc.suitalk.moduleapi.ag.extension;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import cc.suitalk.arbitrarygen.engine.JavaCodeAGEngine;
import cc.suitalk.arbitrarygen.extension.AGContext;
import cc.suitalk.arbitrarygen.extension.AGContextExtension;
import cc.suitalk.arbitrarygen.extension.ArbitraryGenProcessor;
import cc.suitalk.arbitrarygen.impl.AGAnnotationWrapper;
import cc.suitalk.arbitrarygen.template.TemplateManager;
import cc.suitalk.arbitrarygen.utils.FileOperation;
import cc.suitalk.arbitrarygen.utils.JSONArgsUtils;
import cc.suitalk.arbitrarygen.utils.Log;

/**
 * Created by albieliang on 2018/2/1.
 */

public class ModuleApiAGContextExtension implements AGContextExtension {

    private static final String TAG = "Api.ModuleApiAGContextExtension";

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

    @Override
    public void onPreInitialize(AGContext context, JSONObject initArgs) {
        Log.d(TAG, "onPreInitialize(%s)", context.hashCode());
        JSONObject ext = initArgs.getJSONObject("ext");
        if (ext == null || ext.isNullObject()) {
            Log.i(TAG, "onPreInitialize, closure ext do not exist, skip.");
            return;
        }
        JSONObject moduleApi = ext.getJSONObject("moduleApi");
        if (moduleApi == null || moduleApi.isNullObject()) {
            Log.i(TAG, "onPreInitialize, closure moduleApi do not exist, skip.");
            return;
        }
        JSONArray makeApiRules = JSONArgsUtils.getJSONArray(moduleApi, "makeApiRules", true);
        if (makeApiRules == null) {
            makeApiRules = new JSONArray();
        }
        if (makeApiRules.isEmpty()) {
            String srcDir = moduleApi.getString("srcDir");
            if (srcDir == null || srcDir.length() == 0) {
                Log.e(TAG, "onPreInitialize, neither moduleApi.srcDir nor moduleApi.makeApiRules is null or nil, skip.");
                return;
            }
            if (srcDir.endsWith("/")) {
                makeApiRules.add(srcDir + "*");
            } else {
                makeApiRules.add(srcDir + "/*");
            }
        }
        String destDir = moduleApi.getString("destDir");
        if (destDir == null || destDir.length() == 0) {
            Log.e(TAG, "onPreInitialize, moduleApi.destDir is null or nil, skip.");
            return;
        }
        ArbitraryGenProcessor processor = context.getProcessor("javaCodeEngine");
        if (processor instanceof JavaCodeAGEngine) {
            JavaCodeAGEngine engine = (JavaCodeAGEngine) processor;
            AGAnnotationWrapper annWrapper = new AGAnnotationWrapper();
            annWrapper.addAnnotationProcessor(new ModuleApiTaskProcessor());
            engine.addTypeDefWrapper(annWrapper);

            JSONObject javaCodeEngine = initArgs.getJSONObject("javaCodeEngine");
            if (javaCodeEngine == null || javaCodeEngine.isNullObject()) {
                javaCodeEngine = new JSONObject();
            }
            JSONArray ruleArray;
            if (!javaCodeEngine.containsKey("rule")) {
                ruleArray = new JSONArray();
            } else {
                ruleArray = JSONArgsUtils.getJSONArray(javaCodeEngine, "rule", true);
                if (ruleArray == null) {
                    ruleArray = new JSONArray();
                }
            }
            ruleArray.addAll(makeApiRules);
            javaCodeEngine.put("rule", ruleArray);
            initArgs.put("javaCodeEngine", javaCodeEngine);
            context.getKeyValueSet().put("apiDestPath", destDir);
        }
    }
}
