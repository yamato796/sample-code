/**
 * Authenticate to ECR and push image tags.
 *
 * Usage:
 *   ecrPush(
 *       imageRepo: 'my-registry/my-service',
 *       tags:      ['abc1234', 'latest'],
 *   )
 *
 * Reads CI_ECR_REGISTRY, CI_AWS_REGION, CI_ECR_PUSH_ROLE_ARN
 * from Jenkins global env vars. Skips auth for local registries.
 */
def call(Map args) {
    def registry = env.CI_ECR_REGISTRY
    def region   = env.CI_AWS_REGION
    def roleArn  = env.CI_ECR_PUSH_ROLE_ARN

    assert args.imageRepo : 'ecrPush requires imageRepo'
    assert args.tags      : 'ecrPush requires tags'

    // Skip auth for local registries (docker-compose dev environment).
    if (!(registry =~ /^(localhost|127\.0\.0\.1|registry):/)) {
        def loginCmd = "set +x; aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${registry}"
        if (roleArn) {
            withAWS(role: roleArn, region: region, duration: 900) {
                sh loginCmd
            }
        } else {
            sh loginCmd
        }
    }

    try {
        args.tags.each { t ->
            sh "docker push ${args.imageRepo}:${t}"
        }
    } finally {
        sh "docker logout ${registry} || true"
    }
}
