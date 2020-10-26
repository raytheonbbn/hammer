import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

class TakSdkExtension {
    boolean haveRemoteRepo
    boolean mavenOnly
    boolean snapshot

    String takrepoUrl
    String takrepoUser
    String takrepoPassword

    String localSdkPath
}

class TakSdkPlugin implements Plugin<Project> {
    void apply(Project project) {
        def extension = project.extensions.create('takSdk', TakSdkExtension)

        extension.takrepoUrl = getLocalOrProjectProperty(project, 'takrepo.url', null)
        extension.takrepoUser = getLocalOrProjectProperty(project, 'takrepo.user', null)
        extension.takrepoPassword = getLocalOrProjectProperty(project, 'takrepo.password', null)
        extension.mavenOnly = getLocalOrProjectProperty(project, 'takrepo.force', 'false').equals('true')
        extension.snapshot = getLocalOrProjectProperty(project, 'takrepo.snapshot', 'true').equals('true')

        extension.haveRemoteRepo = !((null == extension.takrepoUrl) || (null == extension.takrepoUser) || (null == extension.takrepoPassword))

        if(extension.mavenOnly) {
            // user has specified that the SDK should only be pulled from Maven
            configureMaven(project)
        } else if(isAutobuilder(project)) {
            configureAutobuild(project)
        } else if(isOfflineSdk(project)) {
            configureOfflineSdk(project)
        } else if(extension.haveRemoteRepo) {
            configureMaven(project)
        } else {
            throw new GradleException("Unable to configure TAK SDK")
        }
    }

    String getLocalOrProjectProperty(Project project, String key, String defval) {
        if (new File('local.properties').exists()) {
            def localProperties = new Properties()
            localProperties.load(project.rootProject.file('local.properties').newDataInputStream())
            def value = localProperties.get(key)
            if ((null != value)) {
                return value
            }
        }
        return project.properties.get(key, defval)
    }

    boolean isAutobuilder(Project project) {
        return new File("${project.rootDir}/../../ATAK/app/build/libs/main.jar").exists()
    }

    boolean isOfflineSdk(Project project) {
        // configure the several well known local SDK paths
        def localSdkPath = project.properties['sdk.path'] ?: "${project.rootDir}/sdk"
        def localApiFiles = [
            "${project.rootDir}/../../main.jar",
            "$localSdkPath/main.jar"
        ]

        // check the users `local.properties`
        if (new File('local.properties').exists()) {
            def localProperties = new Properties()
            localProperties.load(project.rootProject.file('local.properties').newDataInputStream())
            def sdkProperty = localProperties.get('sdk.path')
            if ((null != sdkProperty) && (new File(sdkProperty, "main.jar").exists())) {
                localApiFiles.add(sdkProperty)
            }
        }

        // see if the offline SDK is available
        for (def index = 0; localApiFiles.size() > index; ++index) {
            def apiFqn = localApiFiles[index]
            if (new File(apiFqn).exists()) {
                return true
            }
        }

        return false
    }

    // `configureXXX` methods need to set dependencies

    void configureAutobuild(Project project) {
        println("Configuring Autobuilder TAK plugin build")

        project.android.applicationVariants.all { variant ->
            def devType = variant.buildType.matchingFallbacks[0] ?: variant.buildType.name

            // ATAK Plugin API
            def apiJarName = "${variant.flavorName}-${devType}-${project.version}-api.jar"
            def apiFqn = "${project.rootDir}/../../ATAK/app/build/libs/main.jar"
            if (new File(apiFqn).exists()) {
                project.copy {
                    from apiFqn
                    into project.buildDir
                    rename {
                        return apiJarName
                    }
                }
                // add the JAR file as a dependency
                Configuration config = project.configurations.getByName("${variant.name}CompileOnly")
                Dependency dep = project.dependencies.create(project.fileTree(dir: project.buildDir, include: apiJarName))
                config.dependencies.add(dep)
            } else {
                println("${variant.name} => WARNING: no API file could be established, compilation will fail")
            }

            if ('release' == variant.buildType.name) {
                project."pre${variant.name.capitalize()}Build".doFirst {
                    // Proguard mapping; flavor specific
                    def mappingName = "proguard-${variant.flavorName}-${variant.buildType.name}-mapping.txt"
                    def mappingFqn = "${project.rootDir}/../../ATAK/app/build/outputs/mapping/release/mapping.txt"
                    if (new File(mappingFqn).exists()) {
                        project.copy {
                            from mappingFqn
                            into project.buildDir
                            rename {
                                return mappingName
                            }
                        }
                        mappingFqn = "${project.buildDir}/${mappingName}"
                        println("${variant.name} => found mapping at ${mappingFqn}")
                    } else {
                        file(mappingFqn).text = ""
                        println("${variant.name} => WARNING: no mapping file could be establishd, obfuscating just the plugin to work with the development core")
                    }
                    System.setProperty("atak.proguard.mapping", mappingFqn)

                    // Keystore

                    def storeName = 'android_keystore'
                    def storeFqn = "${project.rootDir}/../../android_keystore"
                    if (new File(storeFqn).exists()) {
                        project.copy {
                            from storeFqn
                            into project.buildDir
                            rename {
                                return storeName
                            }
                        }
                        storeFqn = "${project.buildDir}/${storeName}"
                        println("${variant.name} => found store at ${storeFqn}")
                    } else {
                        println("${variant.name} => WARNING: no keystore could be establishd, signing will fail")
                    }
                }
            }
        }
    }

    void configureOfflineSdk(Project project) {
        println("Configuring Offline SDK TAK plugin build")

        def localApiFiles = [
                "${project.rootDir}/../..",
                "${project.sdkPath}"
        ]

        if (new File('local.properties').exists()) {
            def localProperties = new Properties()
            localProperties.load(project.rootProject.file('local.properties').newDataInputStream())
            def sdkProperty = localProperties.get('sdk.path')
            if ((null != sdkProperty) && (new File(sdkProperty).exists())) {
                localApiFiles.add(sdkProperty)
            }
        }

        for (def index = 0; localApiFiles.size() > index; ++index) {
            def apiFqn = localApiFiles[index]
            if (new File(apiFqn, "main.jar").exists()) {
                switch (index) {
                    case 0:
                        println("Using using the sdk version of main.jar, ${apiFqn}")
                        break;
                    case 1:
                        println("Using local version of main.jar from gradle 'sdk.path', ${apiFqn}")
                        break;
                    case 2:
                        println("Using local version of main.jar from local.properties 'sdk.path', ${apiFqn}")
                        break;
                    default:
                        throw new GradleException("Logic error in main.jar file list")
                }

                project.takSdk.localSdkPath = localApiFiles[index]
                break;
            }
        }

        project.android.applicationVariants.all { variant ->
            def devType = variant.buildType.matchingFallbacks[0] ?: variant.buildType.name

            // ATAK Plugin API
            def apiJarName = "${variant.flavorName}-${devType}-${project.version}-api.jar"
            def apiFqn = "${project.takSdk.localSdkPath}/main.jar"
            if (new File(apiFqn).exists()) {
                project.copy {
                    from apiFqn
                    into project.buildDir
                    rename {
                        return apiJarName
                    }
                }

                // add the JAR file as a dependency
                Configuration config = project.configurations.getByName("${variant.name}CompileOnly")
                Dependency dep = project.dependencies.create(project.fileTree(dir: project.buildDir, include: apiJarName))
                config.dependencies.add(dep)
            } else {
                println("${variant.name} => WARNING: no API file could be establishd, compilation will fail")
            }
        }

        project.android.applicationVariants.all { variant ->
            if ('release' == variant.buildType.name) {
                project."pre${variant.name.capitalize()}Build".doFirst {
                    // Proguard mapping; flavor specific
                    def mappingName = "proguard-${variant.flavorName}-${variant.buildType.name}-mapping.txt"

                    def mappingFqn =  "${project.takSdk.localSdkPath}/mapping.txt"
                    if (new File(mappingFqn).exists()) {
                        project.copy {
                            from mappingFqn
                            into project.buildDir
                            rename {
                                return mappingName
                            }
                        }
                        mappingFqn = "${project.buildDir}/${mappingName}"
                        println("${variant.name} => found mapping at ${mappingFqn}")
                    } else {
                        project.file(mappingFqn).text = ""
                        println("${variant.name} => WARNING: no mapping file could be established, obfuscating just the plugin to work with the development core")
                    }

                    System.setProperty("atak.proguard.mapping", mappingFqn)

                    // Keystore
                    def storeName = 'android_keystore'
                    def storeFqn = "${project.takSdk.localSdkPath}/android_keystore"
                    if (new File(storeFqn).exists()) {
                        project.copy {
                            from storeFqn
                            into project.buildDir
                            rename {
                                return storeName
                            }
                        }
                        println("${variant.name} => found store at ${storeFqn}")
                    } else {
                        println("${variant.name} => WARNING: no keystore could be establishd, signing will fail")
                    }
                }
            }
        }
    }

    void configureMaven(Project project) {
        println("Configuring Maven TAK plugin build")

        // add the maven repo as a dependency
        MavenArtifactRepository takrepo = project.repositories.maven( {
            url = project.takSdk.takrepoUrl
            name = 'takrepo'
            credentials {
                username project.takSdk.takrepoUser
                password project.takSdk.takrepoPassword
            }
        } )
        project.repositories.add(takrepo)

        ////////////////////////////////////////////////////////////////
        project.android.applicationVariants.all { variant ->

            def devType = variant.buildType.matchingFallbacks[0] ?: variant.buildType.name
            def devFlavor = variant.productFlavors.matchingFallbacks[0][0] ?: variant.flavorName
            def stableQualifier = project.takSdk.snapshot ? '-SNAPSHOT' : '.+'
            def mavenTuple = "com.atakmap.app:${devFlavor}-${devType}:${project.atakVersion}${stableQualifier}"

            if (project.takSdk.haveRemoteRepo) {
                // add the Maven tuple as a dependency
                Configuration config = project.configurations.getByName("${variant.name}CompileOnly")
                Dependency dep = project.dependencies.create("${mavenTuple}:api@jar")
                config.dependencies.add(dep)

                println("${variant.name} => Using repository API, ${mavenTuple}:api@jar")

                // add the Maven zip as a dependency
                Configuration apkConfig = project.configurations.create("${variant.name}${project.atakApkConfiguration}")
                apkConfig.dependencies.add(project.dependencies.create("${mavenTuple}@zip"))

                if ('release' == variant.buildType.name) {
                    // these configurations may not be used if we have local assets
                    // however they need to be defined so that they can be resolved during execution
                    Configuration mappingConfig = project.configurations.create("${variant.name}${project.mappingConfiguration}")
                    mappingConfig.dependencies.add(project.dependencies.create("${mavenTuple}:mapping@txt"))
                    Configuration keystoreConfig = project.configurations.create("${variant.name}${project.keystoreConfiguration}")
                    keystoreConfig.dependencies.add(project.dependencies.create("${mavenTuple}:keystore@"))
                }
            } else {
                println("${variant.name} => WARNING: no API file could be established, compilation will fail")
            }
        }

        //////////////////////////////////////////////////////////////////

        project.android.applicationVariants.all { variant ->
            // assembleXXX copies APK and mapping artifacts into output directory
            project."assemble${variant.name.capitalize()}".doLast {
                def zipName = "atak-${variant.flavorName}-${variant.buildType.name}-apk.zip"
                def zipPath = "${project.buildDir}/intermediates/atak-zips"

                project.mkdir zipPath
                project.copy {
                    from project.configurations."${variant.name}${project.atakApkConfiguration}"
                    into zipPath
                    rename {
                        return zipName
                    }
                }
                project.copy {
                    from project.zipTree("${zipPath}/${zipName}")
                    into "${project.buildDir}/outputs/apk/${variant.flavorName}/${variant.buildType.name}"
                    exclude 'mapping.txt'
                    rename 'output.json', 'atak-output.json'
                }
                project.copy {
                    from project.zipTree("${zipPath}/${zipName}")
                    into "${project.buildDir}/outputs/mapping/${variant.name}"
                    include 'mapping.txt'
                    rename 'mapping.txt', 'atak-mapping.txt'
                }
                project.delete zipPath
            }

            // for release, copy input mapping and keystore
            if ('release' == variant.buildType.name) {
                project."pre${variant.name.capitalize()}Build".doFirst {

                    // Proguard mapping; flavor specific
                    def mappingName = "proguard-${variant.flavorName}-${variant.buildType.name}-mapping.txt"
                    def mappingFqn = "${project.buildDir}/${mappingName}"
                    project.copy {
                        from project.configurations."${variant.name}${project.mappingConfiguration}"
                        into project.buildDir
                        rename { sourceName ->
                            println("${variant.name} => Copied proguard mapping ${sourceName} from repository into ${mappingFqn}")
                            return mappingName
                        }
                    }

                    System.setProperty("atak.proguard.mapping", mappingFqn)

                    // Keystore
                    def storeName = 'android_keystore'
                    def storeFqn = "${project.buildDir}/${storeName}"
                    project.copy {
                        from project.configurations."${variant.name}${project.keystoreConfiguration}"
                        into project.buildDir
                        rename { sourceName ->
                            println("${variant.name} => Copied keystore from repository ${sourceName} into ${storeFqn}")
                            return storeName
                        }
                    }
                }
            }
        }
    }
}