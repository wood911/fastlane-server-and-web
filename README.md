# 超级签系统服务器app-sign-server

### 流程简介
#### 1. 公司内网开发机(Mac)
安装好Jenkins，配置好xcode和Android、fastlane环境，通过fastlane上传ipa/apk到`app-sign-server`中 <br>
在开发机上安装Jenkins自行Google，从git上clone你的iOS/Android项目到本地，在项目根目录中配置好fastlane脚本。
这是iOS和Android项目的完整配置，可以下载下来根据自己的需要修改 <br>
[fastlane-ios-config](fastlane-ios-config.zip) ([ios部分配置预览](Fastfile-ios.md)) <br>
[fastlane-android-config](fastlane-android-config.zip)  ([android部分配置预览](Fastfile-android.md))<br>
<br>
[超级签系统实现细节](sign-detail.md)


#### 2. app-sign-server(java-web服务)
把app-sign-server部署到外网Linux上，这个Java-web服务负责上传接口、接收苹果回调过来的udid记录下来并签名第一步上传过来的ipa


#### 3. index-html(呈现给用户下载的页面)
这是一个react-html项目，也是部署到外网Linux上(最好和`app-sign-server`同一台)，Nginx代理后打开可以看到第一步中上传的ios/android下载页。
iOS扫码打开显示iOS项目，Android扫码打开显示Android项目，PC打开可以看到所有的。其中iOS设备安装扫码会提示你安装描述文件，安装后获取udid，
苹果会回调给app-sign-server，接着就会调用`fastlane-onLinux`服务对IPA重新签名，签名完成后重定向到下载页iOS设备就可以安装了


#### 4. fastlane-onLinux(linux上可以跑的fastlane)

参考：https://github.com/wood911/fastlane-onLinux

如果需要集群的话，证书需要通过 AWS / minion / Aliyun 实现同步管理，如下是minion实现(AWS兼容minio)

https://github.com/wood911/fastlane-onLinux/blob/main/resign/fastlane/Fastfile

```ruby
# -*- coding: UTF-8 -*-
# fastlane配置文件，定义fastlane可执行的动作
fastlane_require "fileutils"
fastlane_require "json"

default_platform(:ios)

def fetch_info_plist_with_rubyzip(path)
  info_plist = File::dirname(path) + "/Info.plist"
  Zip::File.open(path, "rb") do |zipfile|
    file = zipfile.glob('**/Payload/*.app/Info.plist').first
    return nil unless file
    File.write(info_plist, zipfile.read(file))
  end
end

def resign_app(params)
  # fastlane ios resign_app app_identifier:$1 version:$2 udid:$3 channelcode:$4 domain:$5 path:$6
  UI.message "====== resign_app ======\nparams入参：#{params}\n"
  # ENV["FL_RESIGN_PARAMS"] = params.to_s
  udid = params[:udid].to_s
  # 读取account.json账号配置信息
  account_path = "./account.json"
  accounts = eval(File.read(account_path))
  unless udid.empty?
    index = accounts.index { |e| e[:udids].include?(udid) }
    # 账号列表中没有找到udid说明是新设备，需要加入到账号中并签名覆盖之前的包
    if index.nil? # 需要重签
      UI.message("【需要重签】账号列表中没有找到udid(#{udid})")
      # 找出udid数小于limit(95)的账号，并把udid添加在该账号下面
      index = accounts.index { |e| e[:udids].length < ENV["UDID_LIMIT"].to_i }
      if index.nil?
        # 没有找到，说明账号都满了，那么从第一个账号devices中替换一个
        index = 0
        accounts[index][:udids].pop()
      end
      accounts[index][:udids].push(udid)
      sign_for_account(accounts[index], params)
      # 签名完成才能写入accounts
      File.write(account_path, accounts.to_json)
    else
      UI.message("【无需重签】udid(#{udid})已存在于该账号(#{accounts[index][:apple_id]})中")
    end
  else
    UI.message("没有传入udid，说明是新版本，要给所有账号重签")
  end

end

def sign_for_account(account, params)
  udid = params[:udid].to_s
  # 原包位置[/root/appsign/appdata/,ios/TianXin/20211110034056/]
  app_path = "#{ENV["APP_PATH"]}#{params[:path]}"
  file_path = Dir.glob("#{app_path}*.ipa").first
  UI.message("origin ipa path:#{file_path}")

  dirname = File::dirname(file_path) + "/#{account[:apple_id]}/"
  sh("rm -rf #{dirname} && mkdir -p #{dirname} && cp -rf #{file_path} #{dirname}")
  file_path = dirname + File::basename(file_path)
  UI.message("target ipa path:#{file_path}")

  # 获取Info.plist
  fetch_info_plist_with_rubyzip(file_path)
  app_identifier = params[:app_identifier] || get_ipa_info_plist_value(ipa: file_path, key: "CFBundleIdentifier").to_s
  display_name = get_ipa_info_plist_value(ipa: file_path, key: "CFBundleDisplayName").to_s
  version = get_ipa_info_plist_value(ipa: file_path, key: "CFBundleShortVersionString").to_s

  # 证书路径，以账号分组管理
  cert_path = FileUtils.pwd + "/../cert/#{account[:apple_id]}"
  unless File::exist?(cert_path)
    FileUtils.mkdir_p(cert_path)
  end

  # 认证类型 0：用户名密码  1：p8私钥
  auth_type = ENV["AUTH_TYPE"].to_i
  if auth_type == 1
    app_store_connect_api_key(
      key_id: account[:key_id],
      issuer_id: account[:issuer_id],
      key_filepath: "../p8/AuthKey_#{account[:key_id]}.p8",
      duration: 1200, # optional (maximum 1200)
      in_house: false # optional but may be required if using match/sigh
    )
  end

  # 注册设备
  devices = account[:udids].map{ |v| {v => v} }.reduce({}){ |h, v| h.merge v }
  register_devices(
    devices: devices,
    username: account[:apple_id],
    team_id: account[:team_id],
    platform: "ios"
  )

  # 检查appId
  produce(
    username: account[:apple_id],
    app_identifier: app_identifier,
    app_name: app_identifier.gsub('.', ''),
    skip_itc: true
  )

  # 同步证书到git
  match(
    app_identifier: app_identifier,
    type: "adhoc",
    force_for_new_devices: true,
    username: account[:apple_id],
    team_id: account[:team_id],
    output_path: cert_path
  )

  # 同步证书到minion
  # match(
  #   storage_mode: "s3",
  #   s3_region: ENV["S3_REGION"].to_s,
  #   s3_access_key: ENV["S3_ACCESS_KEY"].to_s,
  #   s3_secret_access_key: ENV["S3_SECRET_ACCESS_KEY"].to_s,
  #   s3_bucket: ENV["S3_BUCKET"].to_s,
  #   s3_object_prefix: "",
  #   s3_endpoint: ENV["S3_ENDPOINT"].to_s,
  #   app_identifier: app_identifier,
  #   type: "adhoc",
  #   force_for_new_devices: true,
  #   username: p8_auth ? nil : apple_id,
  #   team_id: team_id,
  #   output_path: cert_path
  # )

  # 生成证书
  # cert(
  #   username: apple_id,
  #   team_id: team_id,
  #   output_path: cert_path
  # )
  # 生成profile
  # sigh(
  #   app_identifier: app_identifier,
  #   adhoc: true,
  #   force: false,
  #   username: apple_id,
  #   team_id: team_id,
  #   skip_install: true,
  #   skip_certificate_verification: true,
  #   output_path: cert_path
  # )
  # 依据版本号做版本管理


  # 获取Jenkins中的渠道号
  channelcode = params[:channelcode].to_s
  plist_content = ""
  unless channelcode.empty?
    plist_content = "  <key>ym.app.channelCode</key>\n    <string>#{channelcode}</string>\n  "
  end
  unless udid.empty?
    plist_content = "#{plist_content}  <key>ym.app.udid</key>\n    <string>#{udid}</string>\n  "
  end
  # 保存原目录路径
  pwd = FileUtils.pwd
  # 切换ipa所在目录
  FileUtils.cd(File::dirname(file_path))
  ipa_name = File::basename(file_path)

  # 获取p12和profile
  p12_path = ENV["PRIVATE_KEY_PATH"].to_s
  unless File::exist?(p12_path)
    UI.user_error!("p12 not exist! (path:#{p12_path})")
  end
  profile_path = ENV["sigh_#{app_identifier}_adhoc_profile-path"].to_s
  match_pwd = ENV["MATCH_PASSWORD"].to_s
  log_to_null = ENV["VERBOSE"] ? "" : "> /dev/null 2>&1"
  if !plist_content.empty?
    # 单个设备
    plist_content = "#{plist_content}</dict>\n</plist>"
    UI.verbose(plist_content)
    sh("unzip #{ipa_name} #{log_to_null}")
    # 写入channelCode到Info.plist中
    temp_plist = "temp.plist"
    sh("cp -rf Info.plist #{temp_plist}")
    if File.file?(temp_plist)
      contents = File.read(temp_plist)
      contents = contents.gsub(/<\/dict>\n<\/plist>/, plist_content)
      File.write(temp_plist, contents)
      UI.verbose(contents)
    end
    sh("cp -rf #{temp_plist} Payload/#{File.basename(ipa_name, ".ipa")}.app/Info.plist")
    sh("rm -rf #{ipa_name} && zip -q -r #{ipa_name} Payload")
    # 签名渠道包
    sh("#{pwd}/../zsign -k #{p12_path} -p #{match_pwd} -m #{profile_path} -b #{app_identifier} -o output.ipa -z 9 #{ipa_name}")
    sh("rm -rf #{ipa_name} Payload temp.plist Info.plist && mv output.ipa #{ipa_name}")
  elsif
    # 升级
    sh("#{pwd}/../zsign -k #{p12_path} -p #{match_pwd} -m #{profile_path} -b #{app_identifier} -o output.ipa -z 9 #{ipa_name}")
    sh("rm -rf #{ipa_name} Info.plist && mv output.ipa #{ipa_name}")
  end

  # 创建manifest.plist
  domain = params[:domain] || ENV["DOWNLOAD_DOMAIN"].to_s
  # https://woodtechblog.tk/app/download/ios/TianXin/20211110034056/TianXin.ipa
  download_url = domain + "#{file_path}".gsub(ENV["APP_PATH"].to_s, '')
  UI.message("download url:#{download_url}")
  string = create_manifest(download_url, params[:icon], app_identifier, version, display_name)
  File.write("manifest.plist", string)

  # 任务执行完后切换到上一次目录
  FileUtils.cd(pwd)
end

def create_manifest(url, icon, bundle_id, version, title)
  string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">
<plist version=\"1.0\">
    <dict>
        <key>items</key>
        <array>
            <dict>
                <key>assets</key>
                <array>
                    <dict>
                        <key>kind</key>
                        <string>software-package</string>
                        <key>url</key>
                        <string>#{url}</string>
                    </dict>
                    <dict>
                        <key>kind</key>
                        <string>display-image</string>
                        <key>needs-shine</key>
                        <true/>
                        <key>url</key>
                        <string>#{icon}</string>
                    </dict>
                </array>
                <key>metadata</key>
                <dict>
                    <key>bundle-identifier</key>
                    <string>#{bundle_id}</string>
                    <key>bundle-version</key>
                    <string>#{version}</string>
                    <key>kind</key>
                    <string>software</string>
                    <key>platform-identifier</key>
                    <string>com.apple.platform.iphoneos</string>
                    <key>title</key>
                    <string>#{title}</string>
                </dict>
            </dict>
        </array>
    </dict>
</plist>"
  return string
end


############################################### iOS #############################################
platform :ios do

  # 所有lane动作开始前都会执行这里的命令，例如指定打master上的包或执行project clean
  before_all do |options|

  end

  desc "resign an ios app"
  lane :resign_app do|option|
    resign_app(option)
  end

  desc "nuke certificate"
  lane :nuke_cert do|option|
    apple_id = option[:apple_id]
    account_path = "./account.json"
    accounts = eval(File.read(account_path))
    account = accounts.find { |e| e[:apple_id].include?(apple_id) }
    if account.nil?
      UI.user_error!("账户(#{apple_id})不存在")
    end
    app_identifier = option[:app_identifier]
    match_nuke(
      app_identifier: app_identifier,
      type: "adhoc",
      username: apple_id,
      team_id: account[:team_id],
      output_path: "../cert/#{apple_id}"
    )
  end

end

```