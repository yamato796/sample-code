// Build production Docker image only. No tests, no push.
// Use case: verify Dockerfile builds cleanly, or called as sub-job from ci.groovy.

pipeline {
    agent { label env.RUCKUS_AGENT_LABEL ?: 'linux-docker' }

    parameters {
        string(name: 'GIT_REF',      defaultValue: 'main', description: 'Branch or commit to build')
        string(name: 'SERVICE_NAME', defaultValue: 'simple-go-server')
        string(name: 'DOCKERFILE',   defaultValue: 'Dockerfile')
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/yamato796/sample-code.git',
                    branch: params.GIT_REF
            }
        }

        stage('Build') {
            steps {
                script {
                    def sha  = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def repo = "${env.RUCKUS_ECR_REGISTRY}/${params.SERVICE_NAME}"

                    throttle(['docker-build']) {
                        lock(resource: "docker-${env.NODE_NAME}") {
                            sh "docker build -f ${params.DOCKERFILE} -t ${repo}:${sha} ."
                        }
                    }

                    echo "Built ${repo}:${sha}"
                }
            }
        }
    }

    post {
        always { cleanWs() }
    }
}
