// IFileService.aidl
package me.simpleHook.platform.shizuku;

// Declare any non-default types here with import statements

interface IFileService {

    List<String> listFiles(String dirPath);
    boolean copyFile(String scrPath, String desPath);
    boolean writeFile(String path, String content);
    boolean deleteFile(String path);
    boolean deleteFiles(in List<String> paths);
    void forceStopPackage(String packageName);
    void reLaunchApp(String packageName, String activityName);
}
