### fastlane-ios配置文件

Jenkins可以配置定时任务，自动拉取git代码然后执行shell运行fastlane去打包构建并上传app-server，
Jenkins构建任务配置参数Debug/Release、版本号、自定义参数等可以传入fastlane中。<br>

例如：这个项目中要求从Jenkins中获取channelcode并写入Info.plist文件中<br>

```ruby
# -*- coding: UTF-8 -*-
# fastlane配置文件，定义fastlane可执行的动作
fastlane_require "rest-client"
fastlane_require "fileutils"
fastlane_require "json"
fastlane_require "zip"

default_platform(:ios)


def set_teamid
  puts "set_teamid"
  pwd = FileUtils.pwd
  puts pwd
  team_id = CredentialsManager::AppfileConfig.try_fetch_value(:team_id)
  puts "team_id=#{team_id}"
  project = "#{pwd}/../kchat.xcodeproj/project.pbxproj"
  if File.file?(project)
    contents = File.read(project)
    contents = contents.gsub(/DEVELOPMENT_TEAM = \w+;/, "DEVELOPMENT_TEAM = #{team_id};")
    File.write(project, contents)
  end
end

def fetch_info_plist_with_rubyzip(path)
  info_plist = File::dirname(path) + "/Info.plist"
  Zip::File.open(path, "rb") do |zipfile|
    file = zipfile.glob('**/Payload/*.app/Info.plist').first
    return nil unless file
    File.write(info_plist, zipfile.read(file))
  end
end

def build_sign_app(param)
  UI.message "====== build_sign_app ======\nparam入参：#{param}\n"
  increment_version_number(version_number: param[:version], xcodeproj: 'kchat.xcodeproj')
  increment_build_number(build_number: param[:build], xcodeproj: 'kchat.xcodeproj')
  # register_devices应用内测阶段，注册苹果设备ID，可以扫码下载
  apple_id = CredentialsManager::AppfileConfig.try_fetch_value(:apple_id)
  team_id = CredentialsManager::AppfileConfig.try_fetch_value(:team_id)
  app_identifier = CredentialsManager::AppfileConfig.try_fetch_value(:app_identifier)
  produce(
    username: apple_id,
    app_identifier: app_identifier,
    app_name: app_identifier.to_s.gsub('.', ''),
    language: 'en-US',
    skip_itc: true
  )
  register_devices(
      devices_file: "./fastlane/devices.txt",
      team_id: team_id,
      username: apple_id,
      platform: "ios"
  )
  configuration = "Release"
  dirPrefix = "Release_"
  lane_name = lane_context[SharedValues::LANE_NAME].to_s
  if lane_name.include? "debug"
      configuration = "Debug" # flutter不支持debug模式下分发
      dirPrefix = "Beta_"
  end
  # match应用签名，自动生成证书并上传至私有git仓库，保证安全
  match(type: "adhoc", force_for_new_devices: true)
  get_push_certificate(output_path: 'cert', p12_password: '123456')
  scheme = param[:scheme]
  if scheme.nil? || scheme.empty?
    scheme = "kchat"
  end
  build_app(
    export_team_id: team_id,
    export_method: "ad-hoc",
    workspace: "kchat.xcworkspace",
    configuration: configuration,
    scheme: scheme,
    clean: true,
    xcargs: "-allowProvisioningUpdates",
    output_directory: "./build/output/#{dirPrefix}#{Time.now.strftime('%Y%m%d%H%M%S')}",
    output_name: "#{scheme}.ipa"
  )
end


def upload_to_server(param)
  UI.message "====== upload_to_server ======\nparam入参：#{param}\n"
  description = "#{param[:desc]}\n正式环境"
  lane_name = lane_context[SharedValues::LANE_NAME].to_s
  if lane_name.include? "debug"
      description = "#{param[:desc]}\n测试环境"
  end
  scheme = param[:scheme]
  if scheme.nil? || scheme.empty?
    scheme = "kchat"
  end
  platform_name = lane_context[SharedValues::PLATFORM_NAME].to_s
  if platform_name == "ios"
    file_path = lane_context[SharedValues::IPA_OUTPUT_PATH].to_s
    build = get_ipa_info_plist_value(ipa: file_path, key: "CFBundleVersion").to_i
    version = get_ipa_info_plist_value(ipa: file_path, key: "CFBundleShortVersionString").to_s
    bundle_id = get_ipa_info_plist_value(ipa: file_path, key: "CFBundleIdentifier").to_s
  else
    file_path = lane_context[SharedValues::GRADLE_APK_OUTPUT_PATH].to_s
    version = lane_context[SharedValues::ANDROID_NEW_VERSION_NAME].to_s
    build = lane_context[SharedValues::ANDROID_NEW_VERSION_CODE].to_i
    bundle_id = ""
  end
  UI.message "#{version} #{build} #{bundle_id}"
  file_size = File.size(file_path)
  # 获取Info.plist
  info_plist = File::dirname(file_path) + "/Info.plist"
  fetch_info_plist_with_rubyzip(file_path)
  puts info_plist

  # 获取Jenkins中的渠道号
  channel_code = param[:channelcode].to_s
  if !channel_code.empty?
    # 保存原目录路径
    pwd = FileUtils.pwd
    # 切换ipa所在目录
    FileUtils.cd(File::dirname(file_path))
    ipa_name = File::basename(file_path)
    sh("unzip #{ipa_name}")
    channel_code.split(',').each do |e|
      UI.message e
      # 写入channelCode到Info.plist中
      temp_plist = "temp.plist"
      sh("cp -rf Info.plist #{temp_plist}")
      if File.file?(temp_plist)
        contents = File.read(temp_plist)
        contents = contents.gsub(/<\/dict>\n<\/plist>/, "  <key>ym.app.channelCode</key>\n    <string>#{e}</string>\n  </dict>\n</plist>")
        File.write(temp_plist, contents)
        UI.message contents
      end
      sh("cp -rf #{temp_plist} Payload/#{File.basename(ipa_name, ".ipa")}.app/Info.plist")
      sh("zip -q -r #{e}.ipa Payload")
      # 签名渠道包
      sh("fastlane sigh resign #{e}.ipa --signing_identity 'Apple Distribution: Nancy Ying Lin (2255639AQ4)' -p '#{ENV['sigh_com.cao.kchat_adhoc_profile-path']}'")
    end
    sh("rm -rf Payload temp.plist")
    # 任务执行完后切换到上一次目录
    FileUtils.cd(pwd)
  end

  Dir["#{File::dirname(file_path)}/*.ipa"].each do |e|
    url = "https://localios.ystata.xyz/app/app/upload"
    UI.message "开始上传文件到：#{url}"
    response = RestClient::Request.execute(
      log: Logger.new(STDOUT),
      method: :post, url: url,
      timeout: 600, payload: {
      :file => File.new(e, 'rb'),
      :build => build,
      :version => version,
      :size => file_size,
      :bundleId => bundle_id,
      :desc => description
    })
    puts response
  end

  if param[:yunque] == 'test'
    url = "https://yq.k-chat.top/app/app/upload"
  elsif param[:yunque] == 'prod'
    url = "https://yq.benkunmaoyi.top/app/app/upload"
  else
    url = ""
  end
  
if !url.empty?
  UI.message "开始上传文件到：#{url}"
  response = RestClient::Request.execute(
    log: Logger.new(STDOUT),
    method: :post, url: url,
    timeout: 600, payload: {
    :file => File.new(file_path, 'rb'),
    :plistfile => File.new(info_plist, 'rb'),
    :build => build,
    :version => version,
    :size => file_size,
    :bundleId => bundle_id,
    :desc => description
  })
  puts response
end

  sh("git reset --hard && git clean -fd")
end


############################################### iOS #############################################
platform :ios do

  # 所有lane动作开始前都会执行这里的命令，例如指定打master上的包或执行project clean
  before_all do |options|
    #ensure_git_status_clean
    sh("git reset --hard && git clean -fd")
    git_pull
    sh("pod update")
    app_identifier = CredentialsManager::AppfileConfig.try_fetch_value(:app_identifier)
    update_app_identifier(xcodeproj: "kchat.xcodeproj", plist_path: "kchat/Info.plist", app_identifier: app_identifier)
    set_teamid
  end

  desc "构建一个测试环境版本上传至服务器"
  lane :debug do|option|
    build_sign_app(option)
    upload_to_server(option)
  end

  desc "构建一个正式环境版本上传至服务器"
  lane :release do|option|
    build_sign_app(option)
    upload_to_server(option)
  end

  desc "构建一个正式环境版本上传至AppStore"
  lane :appstore do|option|
    ensure_git_branch(branch: '^release|master$')
    increment_build_number(xcodeproj: 'kchat.xcodeproj')
    apple_id = CredentialsManager::AppfileConfig.try_fetch_value(:apple_id)
    team_id = CredentialsManager::AppfileConfig.try_fetch_value(:team_id)
    app_identifier = CredentialsManager::AppfileConfig.try_fetch_value(:app_identifier)
    produce(
      username: apple_id,
      app_identifier: app_identifier,
      app_name: "KO聊",
      language: 'zh-Hans',
      app_version: '1.0.0',
      sku: app_identifier,
      enable_services: {
        push_notification: "on"
      }
    )
    match(type: "appstore")
    build_app(
      export_method: "app-store",
      workspace: "kchat.xcworkspace",
      scheme: option[:scheme],
      configuration: "Release",
      clean: true,
      output_directory: "./build/output/AppStore_#{Time.now.strftime('%Y%m%d%H%M%S')}",
      output_name: "#{option[:scheme]}.ipa"
    )
    upload_to_app_store(
      ipa: lane_context[SharedValues::IPA_OUTPUT_PATH].to_s, 
      submit_for_review: option[:submit],
      submission_information: { 
        export_compliance_uses_encryption: false,
        add_id_info_uses_idfa: false
      }
    )
    git_commit(path: ".", message: "[ci-skip] Bump build")
    push_to_git_remote
  end

end
```



