### fastlane-android配置文件

Jenkins可以配置定时任务，自动拉取git代码然后执行shell运行fastlane去打包构建并上传app-server，
Jenkins构建任务配置参数Debug/Release、版本号、自定义参数等可以传入fastlane中。<br>

```ruby
# -*- coding: UTF-8 -*-
# fastlane配置文件，定义fastlane可执行的动作
require 'rest-client'

default_platform(:android)

def upload_to_server(param)
  UI.message "====== upload_to_server ======\nparam入参：#{param}\n"
  string = 'YM体育'
  if param[:scheme].to_s.downcase.include? "app"
    string = 'YM全站'
  end
  description = "#{param[:desc]}#{string}\n正式环境"
  lane_name = lane_context[SharedValues::LANE_NAME].to_s
  if lane_name.include? "debug"
      description = "#{param[:desc]}#{string}\n测试环境"
  end
  file_path = lane_context[SharedValues::GRADLE_APK_OUTPUT_PATH].to_s
  # versionName、versionCode先从output.json文件里取
  version = '1.0.0'
  build = '1'
  bundle_id = ""
  apk_output_json_path = File.join(File.dirname(file_path), "output-metadata.json")
  if File.readable?(apk_output_json_path)
    apk_output_content = File.read(apk_output_json_path)
    puts apk_output_content
    version_number_regex = /"versionName": "(.*)"/.match(apk_output_content)
    build_number_regex = /"versionCode": (\d+)/.match(apk_output_content)
    bundle_id_regex = /"applicationId": "(.*)"/.match(apk_output_content)
    if !version_number_regex.nil? && !build_number_regex.nil?
      version = version_number_regex.captures.first
      build = build_number_regex.captures.first
    end
    if !bundle_id_regex.nil?
      bundle_id = bundle_id_regex.captures.first
    end
   end
  UI.message "#{version} #{build} #{bundle_id}"
  file_size = File.size(file_path)
  
  url = "https://www.xioscdn.com/app/app/upload"
  UI.message "开始上传文件到：#{url}"
  response = RestClient::Request.execute(
    log: Logger.new(STDOUT),
    method: :post, url: url,
    timeout: 600, payload: {
    :file => File.new(file_path, 'rb'),
    :build => build,
    :version => version,
    :size => file_size,
    :bundleId => bundle_id,
    :desc => description
  })
  puts response
  
  sh("git reset --hard")
end

def setting_env(param)
  string = 'val appType = AppConfig.TYPE.SPORT'
  sh("git checkout master-sport")
  if param[:scheme].to_s.downcase.include? "app"
    string = 'val appType = AppConfig.TYPE.APP'
    sh("git checkout master")
  end
  git_pull
  config_file = "#{Dir.pwd}/../../VersionPlugin/src/main/java/com/ym/plugin/BuildConfig.kt"
  File.open(config_file, "r") do |file|
    buffer = file.read.gsub(%r{val appType = AppConfig.TYPE.*\n$}, string)
    puts buffer
    File.open(config_file, "w") { |file|
      file.write(buffer)
    }
  end
end


############################################### Android ##########################################
platform :android do

  before_all do |options|
    ensure_git_branch(branch: '^master-sport|master$')
    sh("git reset --hard")
  end

  desc "构建一个正式环境版本上传至服务器"
  lane :release do|option|
    setting_env(option)
    gradle(task: 'clean assemble', flavor: 'AppYn', build_type: 'Release', project_dir: './')
    upload_to_server(option)
  end

end

```