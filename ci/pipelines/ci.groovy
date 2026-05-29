// Full CI orchestrator — calls sub-jobs then pushes on main.
// Runs as a Multibranch Pipeline: auto-discovers branches and PRs.
// Sub-jobs receive the branch name so they build the correct ref.
@Library('ci-shared-library') _

def serviceName = 'simple-go-server'

pipeline {
    agent { label env.CI_AGENT_LABEL ?: 'linux-docker' }

    options {
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Test') {
            steps {
                build job: "${serviceName}-test",
                      parameters: [string(name: 'GIT_REF', value: env.BRANCH_NAME ?: 'main')],
                      wait: true
            }
        }

        stage('Build Image') {
            steps {
                build job: "${serviceName}-build-image",
                      parameters: [string(name: 'GIT_REF', value: env.BRANCH_NAME ?: 'main')],
                      wait: true
            }
        }

        stage('Push to Registry') {
            when { expression { env.BRANCH_NAME == 'main' } }
            steps {
                script {
                    def sha       = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def imageRepo = "${env.CI_ECR_REGISTRY}/${serviceName}"

                    sh "docker tag ${imageRepo}:${sha} ${imageRepo}:latest"
                    ecrPush(imageRepo: imageRepo, tags: [sha, 'latest'])
                }
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f --filter "until=2h" || true'
            cleanWs()
        }
    }
}
