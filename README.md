# ModuleApi

[![license](http://img.shields.io/badge/license-Apache2.0-brightgreen.svg?style=flat)](https://github.com/AlbieLiang/ModuleApi/blob/master/LICENSE)
[![Release Version](https://img.shields.io/badge/release-0.1.6-red.svg)](https://github.com/AlbieLiang/ModuelApi/releases)
[![wiki](https://img.shields.io/badge/wiki-0.1.6-red.svg)](https://github.com/AlbieLiang/ModuleApi/wiki)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/AlbieLiang/ModuleApi/pulls)

A across module Calling component for Android development.

ModuleApi是一个用于解决Android模块化后，跨模块调用的组件。ModuleApi是基于微信Android终端的[《微信Android模块化架构重构时间》](https://mp.weixin.qq.com/s/6Q818XA5FaHd7jJMFBG60w)分享中的api思想而设计的，并做了一定的优化。


## 配置与使用

在工程的build.gradle中添加依赖

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        // 添加对ModuleApi代码生成插件的依赖
        classpath 'cc.suitalk.tools:module-api-ag-extension:<last-version>'
    }
}
```

在使用ModuleApi的module的build.gradle中添加依赖和配置

```groovy
apply plugin: 'module-api'

dependencies {
    compile 'cc.suitalk.android:module-api:<last-version>'
}

moduleApi {
    // 配置扫描api目录
    srcDir "${project.projectDir.absolutePath}/src/main/api"
    // 配置生成api的目标目录
    destDir "${project.rootProject.projectDir.absolutePath}/api/src/main/api"

    makeApiRules "${project.projectDir.absolutePath}/src/main/api/*.java"
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
## 便捷的功能

### 通过@Autowire标记Api为自动装配接口

若Api（假设为XxxApi）不依赖于任何时机相关的代码逻辑，你可以在该Api的class定义上加上@Autowire标记即可无需手动调用`ModuleApi.set(XxxApi.class, new XxxApiImpl())`函数来设置Api实现，在你第一次调用`ModuleApi.get(XxxApi.class)`时框架将会自动为你构造一个XxxApiImpl实例对象存储到Api管理器中，并且将其返还.


实例如下：
```java
@Autowire
@MakeApi
public class XxxApiImpl implements XxxApi {
    @ApiMethod
    public void invoke() {
    }
}
```

### 通过@DummyWhenNull标记Api实现为空是返回一个Dummy对象

若Api（假设为XxxApi）依赖于某些逻辑的时机来设置具体的接口实例，但是希望在没有调用`ModuleApi.set(XxxApi.class, new XxxApiImpl())`的情况下，调用`ModuleApi.get(XxxApi.class)`也不返回null，则可以通过@DummyWhenNull来标记该Api.

实例如下：
```java
@DummyWhenNull
@MakeApi
public class XxxApiImpl implements XxxApi {
    @ApiMethod
    public void invoke() {
    }
}
```

## 代码混淆

如果你使用到自动装配功能，则需要添加proguard规则
```proguard

# Keep ModuleApi Autowire class
-keep,allowobfuscation @interface cc.suitalk.moduleapi.extension.annotation.Autowire
-keep @cc.suitalk.moduleapi.extension.annotation.Autowire class *
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
