#!/bin/bash
# Wait for the master to generate the SSH pubkey into the shared volume,
# then start the SSH agent.
set -e

echo "[agent] Waiting for SSH public key from master..."
while [ ! -f /ssh-keys/agent-key.pub ]; do sleep 1; done

export JENKINS_AGENT_SSH_PUBKEY=$(cat /ssh-keys/agent-key.pub)
echo "[agent] Got pubkey, starting SSH daemon."

# Fix docker socket permissions (same logic as master bootstrap).
if [ -S /var/run/docker.sock ]; then
    DOCKER_SOCK_GID=$(stat -c '%g' /var/run/docker.sock)
    if ! getent group "${DOCKER_SOCK_GID}" >/dev/null; then
        groupadd -g "${DOCKER_SOCK_GID}" docker-host
    fi
    DOCKER_GROUP_NAME=$(getent group "${DOCKER_SOCK_GID}" | cut -d: -f1)
    usermod -aG "${DOCKER_GROUP_NAME}" jenkins
    chmod 666 /var/run/docker.sock
fi

exec setup-sshd
