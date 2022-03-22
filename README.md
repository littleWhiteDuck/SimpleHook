# SimpleHookShare
# 这里是SimpleHook hook部分代码

# simpleHook使用说明

> [simpleHook.apk](https://wwp.lanzoub.com/b0177tlri)(密码：simple)
>
> tg:@simpleHook

更新中，请先自行摸索

## 1. 功能说明

### 首页

<img src="images/main_home_screenshot.png" style="zoom: 15%;" />

点击加号，可添加配置，点击添加配置进入下面页面

<img src="C:/Users/MLF/Desktop/%E6%9D%82%E4%B8%83%E6%9D%82%E5%85%AB/images/config_screenshot.png" alt="image-20220322191451378" style="zoom:15%;" />

点击‘搜索样式’图标，可进入AppList页面，进行选择应用

点击‘下载样式’图标，可保存配置

点击右下角加号，可在弹出窗口填写配置

<img src="C:/Users/MLF/Desktop/%E6%9D%82%E4%B8%83%E6%9D%82%E5%85%AB/images/config_dialog_screenshot.png" style="zoom:15%;" />

有多种模式可以选择，输入类名前建议了解设置页面（smali to config），它可以简化填写

下面是编写规则：（你可以下载*[HookTest.apk](https://littlewhiteduck.lanzoui.com/b0eqxvvbc)(密码：simple)*，此App应用了所有情况，并内附有配置）

- 支持Java语法和Smali语法填写配置信息

  ```java
  // java
  me.simplehook.MainActivty
  // smali
  Lme/simplehook/MainActivity; //一定要有 --> ; <--
  ```

- 支持基本类型和其它类型

  ```java
  // 类型 主要用于填写参数类型和变量类型
  // 请注意！！！ 暂不支持数组，如：int[]
  // 基本类型你可以这样填
  boolean int long short char byte float double //也可以这样填 Z I J S C B F D
  // 其他类型你可以这样填
  jave.lang.String android.content.Context Ljava/lang/String; Landroid/content/Context;
  ```

- hook返回值

  ```java
  // 返回值类型无需填写，自动推断，下面列出各种类型写法
  /*
  布尔值(boolean、Z)：true、false
  整数(int、I)：1、2、3
  长整型(long、J)：1l、120000L、123456l
  短整型(short、S)：1short、2short 数字后带short
  字符(char、C)：195c、一c 符合char类型的后面带c
  字节(byte、B)：2b、3b 符合byte类型的后面带b
  单浮点(float、F)：2f、3f
  双浮点(double、D): 2d、3d
  其他类型(只能返回null(字符串除外))：null
  字符串(java.lang.String)：不符合上面的全部化为字符串类型
  	其他情况：
  	数字：数字后面加s 
  	布尔：trues、falses
  	null: nulls
  */
  /*
  多个参数参数类型的填法(用英语逗号分开)：
  boolean,int,android.content.Context
  */
  ```

- hook参数值

  ```java
  // 类型值同 hook返回值类型
  //特殊用法，如下面一段代码
  public boolean isModuleLive(Context context, String str, int level){
      
      retrun true
  }
  //如果你只想要hook level的值，你可以在参数值那一栏向下面这样填
  ,,99
  //如果你只想要hook str的值，你可以在参数值那一栏向下面这样填
  ,啦啦啦,
  //如果你只想要hook str、level的值，你可以在参数值那一栏向下面这样填
  ,啦啦啦,99
  //如果你想要全部hook，你可以在参数值那一栏向下面这样填
  null,啦啦啦,99 // context为null也许导致闪退
  /*
  多个参数参数类型的填法(用英语逗号分开)：
  android.content.Context,jave.lang.String,int
  或者如下填写
  Landroid/content/Context;,Ljave/lang/String;int
  */
  ```

- 中断执行

  ```java
  // 如hook返回值或者hook参数值一样填，不需要填写返回值、参数值
  ```

- 静态变量

  ```java
  // 具体的值只支持基本类型，和字符串
  // 变量值的填写 同返回值
  ```

- 变量

