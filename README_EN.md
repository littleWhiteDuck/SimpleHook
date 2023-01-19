**Here is the code for the hook part of SimpleHook**

# simpleHook instructions

[Chinese](README.md)|**English**

> [simpleHook.apk](https://wwp.lanzoub.com/b0177tlri)(password: simple)
>
> TG Group: @simpleHook
>
> **SimpleHook** is mainly simple, just like the name. If you pursue more complex hook operation, it is recommended to use **[jsHook (You can implement very powerful Hook)](https://github.com/Xposed-Modules-Repo/me.jsonet.jshook)**, [Qujing (computer browser operation)](https://github.com/Mocha-L/QuJing); if you pursue more extended functions, it is recommended to use [算法助手]
>
> Function overview: custom return value, parameter value, etc., record common encryption algorithms, toast, dialog, popupwindow, JSONObject creation and put, etc.
>

## 1. Function description

### Page Introduction

**front page**

<img src="images\main_home_screenshot.png" width = "200" />

Click the plus sign to add configuration, click Add configuration to enter the following page

**Configuration page**

<img src="images/config_screenshot.png" width = "200" />

Click the 'Search Style' icon to enter the AppList page and select an application

Click the 'Download Style' icon to save the configuration

Click the plus sign in the lower right corner to fill in the configuration in the pop-up window

<img src="images\config_dialog_screenshot.png" width = "200" />

There are multiple modes to choose from. Before entering the class name, it is recommended to understand the settings page (smali to config), which can simplify filling.

**Extended page**

<img src="images\main_extension_screenshot.png" width = "200" />

**Specific function**

<img src="images\extension_main_features_shot.png" width = "200" />

Click the "Play Style" button to open the floating window (you need to grant the permission of the floating window), and then open the target application, you can display some information (the print parameter value, return value, and most functions of the extended page are enabled)

Floating window

<img src="images\main_extension_print_dialog.png" width = "200" />

## 2. Custom Hook writing rules

The following are the writing rules: (You can download *[HookTest.apk](/HookTest.apk)*, this App applies all the situations and includes configuration)

> Please understand the setting page **[smali to config]** before use, it can simplify your operation (with decompiler apps such as **MT manager**)

### A brief basic introduction

- Support Java syntax and Smali syntax to fill in configuration information

  ````java
  // java
  me.simplehook.MainActivty
  // smali
  Lme/simplehook/MainActivity; //must have --> ; <--
  ````

- Support for primitive types and other type parameters

  ````java
  // Type is mainly used to fill in the parameter type and variable type
  // Basic types you can fill in using java syntax like this
  boolean int long short char byte float double
  // You can also use smali syntax to fill in basic types like this
  Z I J S C B F D
  // For other types, you can use java syntax to fill in
  jave.lang.String android.content.Context
  // You can also use smali syntax to fill in other types like this
  Ljava/lang/String; Landroid/content/Context; //must have --> ; <--
  ````


### Filling rules for result values

  > It should be noted here that this software does not need to fill in the return value and parameter value type like other software, this software does not need it, you only need to **fill in according to the rules**, automatic judgment

#### 2.1. Primitive type

| Type (java, smali) | Examples of value ​​ | Note |
| ------------------ | -------------------- | ------------- |
| boolean (boolean, Z) | true, false | |
| int(int, I) | 1, 2, 3 | |
| long(long, J) | 1l, 120000L, 123456l | Note: Number + 'L' |
| short(short, S) | 1short, 2short | Note: number + 'short' |
| char(char, C) | 195c | Note: conforms to char type string + 'c' |
| byte(byte, B) | 2b, 3b | Note: conforms to byte type string + 'b' |
| float(float, F) | 2f, 3f, 3.0f | Note: number + 'f' |
| double(double, D) | 2d, 3d, 3.0d | Note: number + 'd' |

#### 2.2. null

> Other types can only return null (except strings), null

#### 2.3 Strings

##### 2.3.1 General

> Convert everything that does not conform to the primitive type and null to string type

##### 2.3.2 Special cases

| Special Strings | Examples of Values ​​| Notes |
| ------------ | -------------------------------- | ------------------------------------------ |
| Number | 111s, 2002s | Commonly seen in "111111", but in this software you need to add s after the number, if you don't add s, it will be converted into a number, which may cause the target application to crash |
| Boolean | trues, falses | Common in "true" and "false", but in this software, you need to add s after the boolean value. If you do not add s, it will be converted to a boolean value, which may cause the target application to crash |
| null | nulls | Commonly seen in "null", but in this software, you need to add s after null. If you do not add s, it will be converted to null, which may lead to a null pointer in the target application |
| Empty string | English word 'empty' or Chinese character '空' | If you fill in the blank directly, you will not be able to save the configuration, this is to prevent you from not filling in the modified value when you use it, which will cause the normal Hook |

##### 2.3.3 Random text return value

> Fill in the following json code for the return value, for return value only
>
> ```json
> {
> "random": "abcdefgh123456789",
> "length": 9,
> "key": "key",
> "updateTime": 100,
> "defaultValue": ""
> }
> ````
>
> Introduction to the above json format code:
>
> ```json
> "random": string, fill in which characters the random text consists of
> "length": an integer representing how long random text needs to be generated
> "key": string, unique identification code, can be filled in casually, but when multiple random return values are used in a software, different ones need to be filled in
> "updateTime": an integer, representing how long to update the random text at intervals, in seconds, -1 means update every time
> "defaultValue": optional
> ```

## 3. Hook mode

#### hook return value

````java
 //  eg 1
  import simple.example;
  public class Example{
    public static boolean isFun() {
      boolean result = true;
      ...
       ...
      return result
    }
  }
 /*
  Mode: Hook return value
  The class name should be filled in:  simple.example.Example
  The method name should be filled in:  isFun
  The parameter type should be filled in: Fill in nothing, because this method has no parameters
  The modified value should be filled in: true or false
 */

/*
Filling method of multiple parameter parameter types (separated by English commas, parameter types support arrays):
boolean,int,android.content.Context
*/

// eg 2
  import simple.example;
  public class Example{
    public static String isFun(Sring str, Context context, boolean b) {
      String result = str;
      ...
       ...
      return result;
    }
  }
/*
  Mode：Hook return value
  The class name should be filled in: simple.example.Example
  The method name should be filled in: isFun
  Parameter type should be filled in:
    java syntax: java.lang.String,android.content.Context,boolean (use commas to separate the parameters, only one parameter does not need a comma)
    smali syntax: Ljava/lang/String;,Landroid/content/Context;,Z
  The modified value should be filled: it is a string (should meet the filling rules of the result value, no quotation marks are required)
*/
````

#### hook return value+

>This function can convert json to object (use **Gson**). If you don't know what the Json format of this object looks like, you can use the function [record return value] and copy the return value. This function is not omnipotent and does not apply to all situations. Simple data classes should be no problem, and arrays are not supported for the time being.
>
>Mode: hook return value+
>
>Class name of the return value: fill in the class name of the return value
>
>Modify the value: fill in the json code, such as
>
>````json
>{"isHook":false,"level":10000}
>````
>For example:
>
>```java
>import simple.example;
>
>// data class
>public class UserBean {
>     private boolean isHook;
>     private int level;
>
>     public UserBean(boolean isHook, int level) {
>         this.isHook = isHook;
>         this.level = level;
>     }
>}
>
>public class Example{
>     public static UserBean isFun() {
>       UserBean userBean = new UserBean(true, 10);
>       ...
>        ...
>       return userBean;
>     }
>   }
>/*
>If the return value of hook isFun
>Mode: hook return value +
>Class name: simple.example.Example
>Method name: isFun
>Parameter Type:
>The class name of the return value: simple.example.UserBean
>Result value: {"isHook":false,"level":10000}
>*/
>```
>
>

#### hook parameter value

````java
// The type value is the same as the hook return value type
//Special usage, such as the following code
public boolean isModuleLive(Context context, String str, int level){
  
    retrun true
}
//If you only want the value of the hook level, you can fill in the following in the column to modify the value
,,99
//If you only want the value of hook str, you can fill in the following in the column to modify the value
,hahaha,
//If you only want the value of hook str and level, you can fill in the following in the column to modify the value
,hahaha,99
//If you want all hooks, you can fill in the following in the modified value column
null,hahaha, 99 // context being null may cause a crash
/*
Filling method of multiple parameter parameter types (separated by English commas):
android.content.Context,jave.lang.String,int
Or fill in as follows
Landroid/content/Context;,Ljave/lang/String;int
*/
````

#### break execution

````java
// This mode will intercept method execution
// Fill in the same as the hook return value or hook parameter value, do not need to fill in the return value and parameter value
public void printString() {
    System.out.println("start");
    testBreakMethod();
    System.out.println("end");

    /*
      The output is
      start
      end

      test Break Mode is not output
    */
}

// if: this method is interrupted
public void testBreakMethod() {
    System.out.println("test Break Mode")
}
````

#### Hook all methods with the same name

````java
/*
  Hook all methods of the same name in a class, fill in * for the parameter type
*/
````

#### Hook all methods in a class

````java
/*
  Hook all methods in a class, fill in * for the method name.
  The parameter type can be filled in freely, some hook types cannot be empty.
*/
````

#### Construction method

````java
// Fill in the method name: <init>
import simple.example;
public class Example{
  int a;
  int b;
public Example(int a, boolean b) {
    this.a = a;
    this.b = b
  }
}
// Hook mode, choose according to your own needs, generally the hook parameter value/record parameter value, other modes may cause the software to crash
/*
  Fill in the method name: <init>
  For example, modify two parameter values
    Fill in the class name: simple.example.Example
    Fill in the method name: <init>
    Fill in the parameter type: int, int
    Fill in the result value: 88,99
 */
````

#### HOOK static field

````java
import simple.example;
public class Example{
  public static boolean isTest = false;
}

import simple.example;
public class MainActivity extends Acitvity {
  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
    }

    private void initData(){
      Example.isTest = false;
    }

    private void initView() {
      //You want to change isTest to true, so you should hook this field after assigning a value
      System.out.println(Example.isTest);
    }
}
// Concrete values only support primitive types, and strings
// There is no need to fill in the field type; it must comply with the [result value] filling rules
/*
  Mode: Hook static field
  Hook point: after/before fill in as needed, default after
  The class name should be filled in: simple.example.MainActivity;
  The method name should be filled in: initData
  The parameter type should be filled in: (nothing is filled in, because this method has no parameters)
  The class name where the variable is located: simple.example.Example
  The variable name should be filled in: isTest
  The modified value should be filled in: true/false
*/
````

#### HOOK instance field

````java
import simple.example;
public class UseBean {
    private boolean isHook;
    private int level;

    public UseBean(boolean isHook, int level) {
        this.isHook = isHook;
        this.level = level;
    }

    public boolean isHook() {
        return isHook;
    }

    public void setHook(boolean hook) {
        isHook = hook;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}

import simple.example;
public class MainActivity extends Acitvity {
  private User user;
  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
    }

    private void initData(){
      user = new User(true, 100);
    }

    private void initView() {
      //You want to modify isHook, level, so you should go to hook after this variable is assigned
      System.out.println(user.isHook());
      System.out.println(user.getLevel());
    }
}
// Concrete values only support primitive types, and strings
// There is no need to fill in the field type; it must comply with the [result value] filling rules
/*
  Mode: Hook instance field
  Hook point: after/before fill in as needed, the default is after
  The class name should be filled in: simple.example.UseBean;
  The method name should be filled in: <init> // <init> represents the constructor
  The parameter type should be filled in: boolean, int
  The variable name should be filled in: isHook
  The modified value should be filled in: true/false

  Instance field/member field: cross-class hooks like static field are not supported. You can only hook the field value after a method of this class is executed.
*/
````

#### record parameter value

> The parameter values of the method will be recorded, go to the record page to view,
> If the parameter is an array or list, it will be converted to json format

#### record return value

> The return value of the method will be recorded, go to the record page to view
> If the result is an array or list, it will be converted to json format

#### record return

> The parameter value and return value of the method will be recorded together, go to the record page to view
> If the result is an array or list, it will be converted to json format
> If the parameter is an array or list, it will be converted to json format

#### Extending Hook

> Remember to **turn on the main switch**
> Please go to the app to view the functions

## Frequently Asked Questions (FAQ)

### 1.hook has no effect

> - You can see the xposed framework(example: LSPosed) log, whether there is an error, etc.
>- In some cases, the storage file update configuration needs to be manually refreshed, open, close, edit and save to refresh
> - Please grant the required permissions (below android11: storage permission, android11 and above: ROOT permission)


### 2. What is smali transfer configuration

> After this experimental function is enabled, a 'pasteboard' icon will be added to the top of the configuration page. Click to convert the application code or signature into a configuration (to prevent manual input errors). After adding the configuration, you need to manually select the appropriate mode and result value.
> Example of calling code:
>
> ```smali
> iget v0, p0, Lme/duck/hooktest/bean/UseBean;->level:I
> invoke-virtual {v0}, Lme/duck/hooktest/bean/UseBean;->isHook()Z
> ````
>
> Example of method signature and field signature:
>
> ```smali
> Lme/duck/hooktest/bean/UseBean;->level:I
> Lme/duck/hooktest/bean/UseBean;->isHook()Z
> ````
>
> The above can be selected by long-pressing a field or method in the MT Manager navigation to select **Copy Signature** or **Find Call**

### 3. Why the target application is running slowly

> Please turn off unnecessary **EXTENSION HOOK** and **record parameters**, **record return value**...etc, such as: md5, base64, etc., these functions will generate a lot of Log

### 4.

### 5.

### 6. What is hook point

> Some hooks support manually filling in the hook point. The hook point is the hook before the method is executed or the hook after the method is executed.
>
> before: hook before method execution;
> after: hook after the method is executed

### 7. What is [Remove interfering configuration]

> When you uninstall the app or clear data, the target app configuration file may still be saved in the storage file
>
> 1. /data/local/tmp/simpleHook/target application package name/config/
> 2. /storage/emluated/0/Android/data/target application package name/simpleHook/config/
>
> This function is to traverse all application directories and delete useless configurations (the configuration is displayed in this application).
>
> Because it needs to traverse all applications will be slower