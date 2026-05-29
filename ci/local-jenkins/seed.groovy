// Reads jobs.json and creates Jenkins jobs.
// type=multibranch: auto-discovers branches/PRs/tags
// type=pipeline:    single fixed-branch job (manual trigger)

def jobs = new groovy.json.JsonSlurper().parse(new File('/var/jenkins_home/jobs.json'))

jobs.each { j ->
    if (j.type == 'multibranch') {
        multibranchPipelineJob(j.name) {
            description("CI pipeline for ${j.name}")
            branchSources {
                branchSource {
                    source {
                        git {
                            remote(j.repo)
                            traits {
                                gitBranchDiscovery()
                                gitTagDiscovery()
                            }
                        }
                    }
                }
            }
            factory {
                workflowBranchProjectFactory {
                    scriptPath(j.pipeline)
                }
            }
            orphanedItemStrategy {
                discardOldItems { numToKeep(10) }
            }
        }
    } else {
        pipelineJob(j.name) {
            description("CI pipeline for ${j.name}")
            if (j.trigger) {
                triggers { scm('H/2 * * * *') }
            }
            definition {
                cpsScm {
                    scm {
                        git {
                            remote { url(j.repo) }
                            branches(j.branch)
                        }
                    }
                    scriptPath(j.pipeline)
                    lightweight(true)
                }
            }
        }
    }
}
