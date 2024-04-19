#!groovy

pipeline {
    agent { node { label 'load-master-2' } }
    options {
        buildDiscarder logRotator(numToKeepStr: '100')
    }
    parameters {
        // These settings are used both by the test JVM and by this script too.
        string(defaultValue: 'load-jdk21', description: 'JDK to use', name: 'JDK_TO_USE')

        string(defaultValue: 'load-master-2', description: 'Name of the server machine', name: 'SERVER_NAME')
        string(defaultValue: '-Xms32G -Xmx32G', description: 'Arguments of the server JVM', name: 'SERVER_JVM_OPTS')
        string(defaultValue: 'load-8', description: 'Name of the probe machine', name: 'CLIENT_NAME')
        string(defaultValue: '-Xms8G -Xmx8G', description: 'Arguments of the probe JVM', name: 'CLIENT_JVM_OPTS')
    }
    tools {
        jdk "${JDK_TO_USE}"
    }
    stages {
        stage('appengine-benchmark') {
            agent { node { label "${SERVER_NAME}" } }
            options {
                timeout(time: 10, unit: 'MINUTES')
            }
            steps {
                lock('appengine-benchmark') {
                    // clean the directory before clone
                    sh "rm -rf *"
                    checkout([$class           : 'GitSCM',
                              branches         : [[name: "*/master"]],
                              extensions       : [[$class: 'CloneOption', depth: 1, noTags: true, shallow: true]],
                              userRemoteConfigs: [[url: 'https://github.com/lachlan-roberts/appengine-performance-testing.git']]])
                    withEnv(["JAVA_HOME=${tool "jdk21"}",
                             "PATH+MAVEN=${tool "jdk21"}/bin:${tool "maven3"}/bin",
                             "MAVEN_OPTS=-Xmx4G -Djava.awt.headless=true"]) {
                        configFileProvider(
                            [configFile(fileId: 'oss-settings.xml', variable: 'GLOBAL_MVN_SETTINGS')]) {
                            sh "mvn -ntp -DtrimStackTrace=false -U -s $GLOBAL_MVN_SETTINGS  -Dmaven.test.failure.ignore=true -V -B -e clean test"
                        }
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: "**/target/reports/**/**", allowEmptyArchive: true, onlyIfSuccessful: false
                }
            }
        }
    }
}