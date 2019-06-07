#!/usr/bin/env groovy

pipeline {
    agent any

    tools {
        jdk 'openjdk11'
    }
    environment {
        ZONE = 'fss'
        APPLICATION_NAME = 'altinn-admin'
        DOCKER_SLUG = 'alf'
        FASIT_ENVIRONMENT = 'q1'
        KUBECONFIG = 'kubeconfig-altinnadmin'
    }

    stages {
        stage('initialize') {
            steps {
                init action: 'gradle'
            }
        }
        stage('build and run tests') {
            steps {
                sh './gradlew build'
                slackStatus status: 'passed'
            }
        }
        stage('extract application files') {
            steps {
                sh './gradlew shadowJar'
            }
        }
        stage('push docker image') {
            steps {
                dockerUtils action: 'createPushImage'
            }
        }
        stage('deploy to preprod') {
            steps {
                githubDeploy action: 'create'
                deployApp action: 'kubectlDeploy', cluster: 'dev-fss', placeholderFile: "config-preprod.env"
            }
        }
        stage('deploy to production') {
            when { environment name: 'DEPLOY_TO', value: 'production' }
            steps {
                deployApp action: 'kubectlDeploy', cluster: 'prod-fss', placeholderFile: "config-prod.env"
                githubStatus action: 'tagRelease'
            }
        }
    }
    post {
        always {
            postProcess action: 'always'
            archiveArtifacts artifacts: 'build/reports/rules.csv', allowEmptyArchive: true
        }
        success {
            postProcess action: 'success'
        }
        failure {
            postProcess action: 'failure'
        }
    }
}
