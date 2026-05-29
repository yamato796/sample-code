FROM jenkins/jenkins:lts-jdk17

USER root

# Install Docker CLI from Docker's official repository (the Debian docker.io
# package no longer ships a usable CLI in bookworm). gosu is used by
# bootstrap.sh to drop privileges back to the jenkins user.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg gosu \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
       | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
       https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
       > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && rm -rf /var/lib/apt/lists/* \
    && docker --version

# Skip the setup wizard (JCasC drives everything).
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc.yaml

# Install plugins.
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# JCasC config + seed job definitions.
COPY casc.yaml /var/jenkins_home/casc.yaml
COPY seed.groovy /var/jenkins_home/seed.groovy
COPY jobs.json /var/jenkins_home/jobs.json

# Bootstrap script handles docker socket permissions at startup.
COPY bootstrap.sh /usr/local/bin/bootstrap.sh
RUN chmod +x /usr/local/bin/bootstrap.sh

ENTRYPOINT ["/usr/local/bin/bootstrap.sh"]
