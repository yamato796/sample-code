// Full CI orchestrator — calls sub-jobs then pushes on main/tag.
// Runs as a Multibranch Pipeline: auto-discovers branches/PRs/tags.
// Sub-jobs receive the branch name so they build the correct ref.
@Library('ruckus-ci-shared') _

def serviceName = 'simple-go-server'

pipeline {
    agent { label env.RUCKUS_AGENT_LABEL ?: 'linux-docker' }

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
            when { expression { env.TAG_NAME || env.BRANCH_NAME == 'main' } }
            steps {
                script {
                    def sha  = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def tags = [sha]
                    if (env.TAG_NAME)                   tags << env.TAG_NAME
                    else if (env.BRANCH_NAME == 'main') tags << 'latest'

                    ecrPush(
                        imageRepo: "${env.RUCKUS_ECR_REGISTRY}/${serviceName}",
                        tags:      tags,
                    )
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
