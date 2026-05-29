// Build test image and run lint + unit tests only. No production build, no push.
// Use case: quick feedback during development, or called as sub-job from ci.groovy.

pipeline {
    agent { label env.RUCKUS_AGENT_LABEL ?: 'linux-docker' }

    parameters {
        string(name: 'GIT_REF',        defaultValue: 'main', description: 'Branch or commit to build')
        string(name: 'SERVICE_NAME',    defaultValue: 'simple-go-server')
        string(name: 'TEST_DOCKERFILE', defaultValue: 'Dockerfile.test')
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

        stage('Build Test Image') {
            steps {
                script {
                    def sha = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    env.TEST_IMAGE = "${params.SERVICE_NAME}-test:${sha}"

                    throttle(['docker-build']) {
                        lock(resource: "docker-${env.NODE_NAME}") {
                            sh "docker build -f ${params.TEST_DOCKERFILE} -t ${env.TEST_IMAGE} ."
                        }
                    }
                }
            }
        }

        stage('Quality Gates') {
            parallel {
                stage('Lint') {
                    steps {
                        sh "docker run --rm ${env.TEST_IMAGE} sh -c 'golangci-lint run -v'"
                    }
                }
                stage('Unit Tests') {
                    steps {
                        sh "docker run --rm ${env.TEST_IMAGE} sh -c 'go test -v -cover ./...'"
                    }
                }
            }
        }
    }

    post {
        always { cleanWs() }
    }
}
