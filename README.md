# 超级签系统服务器app-sign-server

### 流程简介
#### 1. 公司内网开发机(Mac)
安装好Jenkins，配置好xcode和Android、fastlane环境，通过fastlane上传ipa/apk到`app-sign-server`中 <br>
在开发机上安装Jenkins自行Google，从git上clone你的iOS/Android项目到本地，在项目根目录中配置好fastlane脚本。
这是iOS和Android项目的完整配置，可以下载下来根据自己的需要修改 <br>
[fastlane-ios-config](fastlane-ios-config.zip) ([ios部分配置预览](Fastfile-ios.md)) <br>
[fastlane-android-config](fastlane-android-config.zip)  ([android部分配置预览](Fastfile-android.md))<br>


#### 2. app-sign-server(java-web服务)
把app-sign-server部署到外网Linux上，这个Java-web服务负责上传接口、接收苹果回调过来的udid记录下来并签名第一步上传过来的ipa
#### 3. index-html(呈现给用户下载的页面)
这是一个react-html项目，也是部署到外网Linux上(最好和`app-sign-server`同一台)，Nginx代理后打开可以看到第一步中上传的ios/android下载页。
iOS扫码打开显示iOS项目，Android扫码打开显示Android项目，PC打开可以看到所有的。其中iOS设备安装扫码会提示你安装描述文件，安装后获取udid，
苹果会回调给app-sign-server，接着就会调用`fastlane-onLinux`服务对IPA重新签名，签名完成后重定向到下载页iOS设备就可以安装了
#### 4. fastlane-onLinux(linux上可以跑的fastlane)
