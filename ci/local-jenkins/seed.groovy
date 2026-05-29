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
                        github {
                            repoOwner(j.owner)
                            repository(j.repo)
                            repositoryUrl(j.url)
                            configuredByUrl(true)
                            traits {
                                gitBranchDiscovery()
                                gitHubPullRequestDiscovery {
                                    strategyId(1)   // merge PR head with target branch
                                }
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
            // Auto-scan for new branches/commits every 2 minutes.
            // In production use 'H/5 * * * *' or rely on webhooks.
            triggers {
                periodicFolderTrigger {
                    interval('2m')
                }
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
                            remote { url(j.url) }
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
