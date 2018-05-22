# ModuleApi

[![license](http://img.shields.io/badge/license-Apache2.0-brightgreen.svg?style=flat)](https://github.com/AlbieLiang/ModuleApi/blob/master/LICENSE)
[![Release Version](https://img.shields.io/badge/release-0.1.0-red.svg)](https://github.com/AlbieLiang/ModuelApi/releases)
[![wiki](https://img.shields.io/badge/wiki-0.1.0-red.svg)](https://github.com/AlbieLiang/ModuleApi/wiki)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/AlbieLiang/ModuleApi/pulls)

A across module Calling component for Android development.

ModuleApi是一个用于解决Android模块化后，跨模块调用的组件。ModuleApi是基于微信Android终端的[《微信Android模块化架构重构时间》](https://mp.weixin.qq.com/s/6Q818XA5FaHd7jJMFBG60w)分享中的api思想而设计的，并做了一定的优化。


## 配置与使用

在工程的build.gradle中添加依赖

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        // 添加对ArbitraryGen的依赖
        classpath 'cc.suitalk.tools:arbitrarygen-plugin:2.1.1'
        // 添加对ModuleApi代码生成插件的依赖
        classpath 'cc.suitalk.tools:module-api-ag-extension:0.1.0'
        ...
    }
}
```

在使用ModuleApi的module的build.gradle中添加依赖和配置

```gralde
apply plugin: 'module-api-ag-extension'
apply plugin: 'arbitrarygen'

dependencies {
    compile 'cc.suitalk.android:module-api:0.1.0'
    ...
}

arbitraryGen {
    templateDir "${project.rootDir.getAbsolutePath()}/ag-template"
    srcDir "${project.projectDir.absolutePath}/ag-datasrc"
    destDir "$buildDir/generated/source/ag-gen"

    ext {
        moduleApi {
            // 配置扫描api目录
            srcDir "${project.projectDir.absolutePath}/src/main/java"
            // 配置生成api的目标目录
            destDir "${project.rootProject.projectDir.absolutePath}/api/build/generated/source/ag-gen"
        }
    }
}
```
## 定义api

#### 在java文件中通过注解标识定义api

```java
@MakeApi
public class ModuleBModel implements ModuleBModelApi {

    @ApiField
    public static final int STYLE_HOLO = 0;

    @ApiField
    public static final int STYLE_DARK = 1;

    @ApiField
    public static final int STYLE_RED = 2;

    @ApiMethod
    public void showToast(int style, String message) {
        // show Toast
    }
}
```
ModuleBModel类的接口ModuleBModelApi将会自动生成到`destDir`目录中

ModuleBModelApi接口：

```java
public interface ModuleBModelApi extends Api {
    public static final String TAG = "AG.ModuleBModelApi";
    public static final int STYLE_HOLO =  0;
    public static final int STYLE_DARK =  1;
    public static final int STYLE_RED =  2;

    void showToast(int style, String message);
}
```

#### 通过.api文件后缀定义api

文件ShowDialogState.api

```java
public enum ShowDialogState {
    SHOW, HIDE
}
```
ShowDialogState.api文件会被拷贝到`destDir`目录中，并修改后缀名为java，生成ShowDialogState.java文件

## 使用api

在使用之前，需要先向ModuleApi中添加各个api接口的实现

```java
ModuleApi.set(ModuleBModelApi.class, new ModuleBModel());
```

在业务逻辑中调用api接口

```java
ModuleApi.get(ModuleBModelApi.class).showToast(ModuleBModelApi.STYLE_RED, "Test from ModuleFirst.");
```

## License

```
   Copyright 2018 Albie Liang

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
