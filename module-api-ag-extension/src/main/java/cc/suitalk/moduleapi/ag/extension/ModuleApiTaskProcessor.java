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

import java.io.File;
import java.util.Set;

import cc.suitalk.arbitrarygen.base.BaseStatement;
import cc.suitalk.arbitrarygen.base.JavaFileObject;
import cc.suitalk.arbitrarygen.block.TypeDefineCodeBlock;
import cc.suitalk.arbitrarygen.core.ArgsConstants;
import cc.suitalk.arbitrarygen.core.Word;
import cc.suitalk.arbitrarygen.expression.VariableExpression;
import cc.suitalk.arbitrarygen.extension.AGContext;
import cc.suitalk.arbitrarygen.extension.CustomizeGenerator;
import cc.suitalk.arbitrarygen.extension.processoing.AGSupportedAnnotationTypes;
import cc.suitalk.arbitrarygen.extension.processoing.AbstractAGAnnotationProcessor;
import cc.suitalk.arbitrarygen.gencode.CodeGenerator;
import cc.suitalk.arbitrarygen.gencode.GenCodeTaskInfo;
import cc.suitalk.arbitrarygen.model.TypeName;
import cc.suitalk.arbitrarygen.protocol.EnvArgsConstants;
import cc.suitalk.arbitrarygen.utils.FileOperation;
import cc.suitalk.arbitrarygen.utils.Log;
import cc.suitalk.arbitrarygen.utils.Util;
import cc.suitalk.moduleapi.extension.annotation.MakeApi;

/**
 * Created by albieliang on 2018/2/1.
 */

@AGSupportedAnnotationTypes({"ApiMethod", "ApiField"})
public class ModuleApiTaskProcessor extends AbstractAGAnnotationProcessor {

    private static final String TAG = "AG.ModuleApiTaskProcessor";

    @Override
    public boolean process(AGContext context, JSONObject env, JavaFileObject javaFileObject, TypeDefineCodeBlock typeDefineCodeBlock, Set<? extends BaseStatement> set) {
        if (typeDefineCodeBlock.getAnnotation(MakeApi.class.getSimpleName()) == null) {
            Log.i(TAG, "the TypeDefineCodeBlock do not contains 'MakeApi' annotation.(%s)", javaFileObject.getFileName());
            return false;
        }
        if (set.isEmpty()) {
            Log.i(TAG, "containsSpecialAnnotationStatements is nil");
            return false;
        }
        final String outputDir = env.optString(EnvArgsConstants.KEY_OUTPUT_DIR);
        final String pkg = env.optString(EnvArgsConstants.KEY_PACKAGE);
        final String filePath = env.optString(EnvArgsConstants.KEY_FILE_PATH);
        if (Util.isNullOrNil(outputDir)) {
            Log.i(TAG, "process failed, outputDir is null or nil.(filePath : %s)", filePath);
            return false;
        }
        String apiDestPath = context.getKeyValueSet().getString("apiDestPath");
        if (apiDestPath == null || apiDestPath.length() == 0) {
            apiDestPath = outputDir;
        }
        JSONObject args = new JSONObject();
//        args.put("template", "");
        args.put("templateTag", "module-api-gentask");
        args.put(ArgsConstants.EXTERNAL_ARGS_KEY_DEST_DIR, apiDestPath);
        args.put("toFile", Util.joint(File.separator, Util.exchangeToPath(pkg),
                String.format("%sApi.java", typeDefineCodeBlock.getName().getName())));
        args.put("fileObject", javaFileObject.toJSONObject());
        args.put("methodSet", toJSONArray(set));
        context.execProcess("template-processor", args);
        if (!containsApiInterface(typeDefineCodeBlock)) {
            if (typeDefineCodeBlock.countOfInterfaces() == 0) {
                typeDefineCodeBlock.setWordImplements(createImplementsWord());
            }
            typeDefineCodeBlock.addInterface(createApiTypeName(typeDefineCodeBlock));

            File file = new File(env.getString(EnvArgsConstants.KEY_FILE_PATH));
            GenCodeTaskInfo taskInfo = new GenCodeTaskInfo();
            taskInfo.FileName = javaFileObject.getFileName();
            taskInfo.RootDir = file.getParentFile().getAbsolutePath();
            taskInfo.javaFileObject = javaFileObject;
            // GenCode
            CustomizeGenerator generator = new CodeGenerator(javaFileObject);
            Log.i(TAG, "genCode rootDir : %s, fileName : %s, suffix : %s", taskInfo.RootDir, taskInfo.FileName, taskInfo.Suffix);
            FileOperation.saveToFile(taskInfo, generator.genCode());
        }
        return true;
    }

    private JSONArray toJSONArray(Set<? extends BaseStatement> set) {
        JSONArray jsonArray = new JSONArray();
        for (BaseStatement s : set) {
            jsonArray.add(s.toJSONObject());
        }
        return jsonArray;
    }

    private boolean containsApiInterface(TypeDefineCodeBlock typeDefineCodeBlock) {
        final String apiName = typeDefineCodeBlock.getName().getName() + "Api";
        for (TypeName tn : typeDefineCodeBlock.getInterfaceList()) {
            if (apiName.equals(tn.getName())) {
                return true;
            }
        }
        return false;
    }

    private TypeName createApiTypeName(TypeDefineCodeBlock typeDefineCodeBlock) {
        TypeName tn = new TypeName();
        Word word = new Word();
        word.value = String.format("%sApi", typeDefineCodeBlock.getName().getName());
        word.blankStr = " ";
        word.type = Word.WordType.STRING;
        tn.setName(new VariableExpression(word));
        return tn;
    }

    private Word createImplementsWord() {
        Word word = new Word();
        word.blankStr = " ";
        word.value = "implements";
        word.type = Word.WordType.STRING;
        return word;
    }
}
