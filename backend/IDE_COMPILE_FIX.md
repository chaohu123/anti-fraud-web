# IDE 编译错误解决方案

## 问题描述
IDE 报错找不到符号或程序包，但实际文件都存在。这是 IDE 的索引/编译缓存问题。

## 解决方案

### IntelliJ IDEA

1. **刷新 Maven 项目**
   - 右键点击 `pom.xml` → `Maven` → `Reload Project`
   - 或打开 Maven 工具窗口，点击刷新按钮

2. **清理并重新构建**
   - `Build` → `Clean Project`
   - `Build` → `Rebuild Project`

3. **使缓存失效并重启**
   - `File` → `Invalidate Caches...` → `Invalidate and Restart`

4. **检查项目设置**
   - `File` → `Project Structure` → `Project`
     - 确保 `Project SDK` 设置为 Java 17
     - 确保 `Project language level` 设置为 17
   - `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Java Compiler`
     - 确保 `Project bytecode version` 设置为 17
     - 确保所有模块的 `Target bytecode version` 设置为 17

5. **重新导入项目**
   - 关闭项目
   - 删除 `.idea` 文件夹（如果存在）
   - 重新打开项目，选择 `Import Maven Project`

### Eclipse

1. **刷新项目**
   - 右键项目 → `Refresh`
   - 右键项目 → `Maven` → `Update Project...`

2. **清理项目**
   - `Project` → `Clean...` → 选择项目 → `Clean`

3. **检查 Java 构建路径**
   - 右键项目 → `Properties` → `Java Build Path`
   - 确保 `JavaSE-17` 在 Libraries 中

### VS Code

1. **重新加载窗口**
   - `Ctrl+Shift+P` → `Developer: Reload Window`

2. **清理 Java 工作区**
   - 删除 `.classpath` 和 `.project` 文件（如果存在）
   - 重新打开项目

## 验证修复

运行以下命令验证项目可以正常编译：

```bash
cd backend
mvn clean compile
```

如果 Maven 编译成功，说明代码没有问题，只是 IDE 的索引需要更新。

## 常见原因

1. IDE 缓存过期
2. Maven 项目未正确导入
3. Java 版本配置不匹配
4. IDE 索引损坏

## 如果问题仍然存在

1. 检查 `pom.xml` 中的 Java 版本配置（应为 17）
2. 确认系统已安装 Java 17
3. 检查 IDE 的 Java 版本设置
4. 尝试在命令行使用 Maven 编译，如果成功，则确定是 IDE 问题

## 如何添加 Java 17 SDK（如果没有）

### 方法 1：在 IDE 中添加 SDK

1. 在项目设置窗口中，点击 SDK 下拉菜单
2. 选择 `+ 添加 SDK` → `下载 JDK...`
3. 选择版本：**17**（LTS）
4. 选择供应商：推荐使用 **Eclipse Temurin** 或 **Oracle OpenJDK**
5. 点击下载并安装

### 方法 2：手动安装 Java 17

1. 下载 Java 17：
   - Oracle OpenJDK: https://jdk.java.net/archive/
   - Eclipse Temurin: https://adoptium.net/temurin/releases/?version=17
   - Amazon Corretto: https://aws.amazon.com/corretto/

2. 安装后，在 IDE 中添加：
   - 点击 `+ 添加 SDK` → `JDK`
   - 选择安装目录（通常是 `C:\Program Files\Java\jdk-17` 或类似路径）

### 临时方案：使用 Java 18

如果暂时无法安装 Java 17，可以使用 **corretto-18**（Amazon Corretto 18）：
- Java 18 可以编译 Java 17 的代码（向后兼容）
- 在 SDK 下拉菜单中选择 `corretto-18 Amazon Corretto version 18.0.2`
- 语言级别设置为 **18** 或 **17**（如果 IDE 支持）

**注意**：虽然可以使用 Java 18/20，但建议尽快安装 Java 17 以确保完全兼容。
