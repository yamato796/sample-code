#!/bin/bash
#
# Local Jenkins bootstrap.
#
# SCM is configured to pull from GitHub directly (casc.yaml), so this script
# only needs to handle Docker socket permissions. The shared library and
# application source are cloned by Jenkins at build time.

set -euo pipefail

# Grant the jenkins user access to the Docker daemon socket.
# On Linux hosts the socket is typically owned by group 'docker' (GID 999),
# so we add jenkins to that group. On macOS (Docker Desktop) the socket is
# root:root with restricted permissions, so we also chmod it as a fallback.
if [ -S /var/run/docker.sock ]; then
    DOCKER_SOCK_GID=$(stat -c '%g' /var/run/docker.sock)
    if ! getent group "${DOCKER_SOCK_GID}" >/dev/null; then
        groupadd -g "${DOCKER_SOCK_GID}" docker-host
    fi
    DOCKER_GROUP_NAME=$(getent group "${DOCKER_SOCK_GID}" | cut -d: -f1)
    usermod -aG "${DOCKER_GROUP_NAME}" jenkins

    # Fallback for macOS Docker Desktop where group membership alone is not
    # sufficient (socket is root:root 0755).
    chmod 666 /var/run/docker.sock
fi

# Chain to the official Jenkins entrypoint as the jenkins user.
exec gosu jenkins /usr/local/bin/jenkins.sh "$@"
