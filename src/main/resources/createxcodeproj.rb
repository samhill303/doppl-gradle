require 'xcodeproj'

# Add the build settings
def setup_build_settings (project, target)
	# Setup variables for build settings stuff
	framework_search_paths = ['$(J2OBJC_LOCAL_PATH)/frameworks']
	linker_flags = ['-lsqlite3', '-lz', '-ObjC']
	arc_value = 'NO'

	lib_paths = []
	header_search_paths = []

	['Mockito', 'JUnit', 'JavaxInject', 'JRE', 'JSR305'].each do |lib|
		# Eventually replace the path up to frameworks with what's in local.properties
		lib_paths.push("/Users/touchlab/Documents/j2objc/dist/frameworks/#{lib}.framework") # CHANGE THIS DIRECTORY ON YOUR MACHINE
		header_search_paths.push("$(J2OBJC_LOCAL_PATH)/frameworks/#{lib}.framework/Headers") # CHANGE THIS DIRECTORY ON YOUR MACHINE
	end

	base_path = '/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk' # CHANGE THIS DIRECTORY ON YOUR MACHINE
	iconv_path = base_path + '/usr/lib/libiconv.tbd'
	uikit_path = base_path + '/System/Library/Frameworks/UIKit.framework'

	lib_paths << iconv_path
	lib_paths << uikit_path

	lib_paths.each do |path|
		libRef = project['Frameworks'].new_file(path)
		# Get the build phase
		framework_buildphase = project.objects.select{|x| x.class == Xcodeproj::Project::Object::PBXFrameworksBuildPhase}[0];

		# Add it to the build phase
		framework_buildphase.add_file_reference(libRef);
	end

	['Debug', 'Release'].each do |config|
		target.build_settings(config)['FRAMEWORK_SEARCH_PATHS'] = framework_search_paths
		target.build_settings(config)['HEADER_SEARCH_PATHS'] = header_search_paths
		target.build_settings(config)['CLANG_ENABLE_OBJC_ARC'] = arc_value
		target.build_settings(config)['OTHER_LDFLAGS'] = linker_flags
	end
end

# Add files and their references to the project
def add_generated_files_to_project (project, target)
	# Create a new folder and add it under that
	main_folder_name = 'Main'
	test_folder_name = 'Test'
	gen_main_group = project.new_group(main_folder_name)
	gen_test_group = project.new_group(test_folder_name)

	# Only using the Droidcon stuff as an example
	main_path = '/Users/touchlab/Downloads/DroidconDopplExample/ios/dcframework/generated' # CHANGE THIS DIRECTORY ON YOUR MACHINE
	test_path = '/Users/touchlab/Downloads/DroidconDopplExample/ios/dcframework/generatedTests' # CHANGE THIS DIRECTORY ON YOUR MACHINE
	addfiles("#{main_path}/*", gen_main_group, target)
	addfiles("#{test_path}/*", gen_test_group, target)
end

def addfiles (direc, current_group, main_target)
    Dir.glob(direc) do |item|
        next if item == '.' or item == '.DS_Store'

                if File.directory?(item)
            new_folder = File.basename(item)
            created_group = current_group.new_group(new_folder)
            addfiles("#{item}/*", created_group, main_target)
        else
          i = current_group.new_file(item)
          main_target.add_file_references([i])

          if item.include? ".properties"
          	build_phase = main_target.resources_build_phase
          	build_phase.add_file_reference(i)
          end
        end
    end
end

# Create a new project
project_path = '/Users/touchlab/Downloads/testproj/ios.xcodeproj' # CHANGE THIS DIRECTORY ON YOUR MACHINE
project = Xcodeproj::Project.new(project_path)

# Build a Framework to put all the doppl stuff in:
new_framework_target = project.new_target(:framework, 'testgenframe', :ios, nil)

add_generated_files_to_project(project, new_framework_target)
setup_build_settings(project, new_framework_target)

# Save the project
project.save