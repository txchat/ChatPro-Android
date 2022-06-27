# 去中心化版本Chat33Pro

## 创建新渠道项目步骤
1. 在[flavor.gradle](./flavor.gradle)的```channel```维度中新增一项
    ```groovy
    newProject {
        dimension "channel"
    }
    ```
2. 在```app```模块中添加新的启动图，在```chat-ui```模块中添加新的启动图标，在```app```模块的[build.gradle](./app/build.gradle)中编辑新项目的应用包名和版本号

3. 在各个需要的模块中添加对应的项目资源文件夹以替换相应的资源

4. 在```business```, ```lib-push```, ```chat-core```, ```chat-oa```, ```oss```, ```wallet```模块中添加对应的properties文件，修改相应的配置