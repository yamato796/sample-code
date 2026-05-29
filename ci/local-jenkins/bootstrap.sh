#!/bin/bash
set -euo pipefail

# --- Generate SSH key pair for agent communication ---
# Written to a shared volume so the agent container can read the pubkey.
if [ ! -f /ssh-keys/agent-key ]; then
    echo "[bootstrap] Generating SSH key pair for agent..."
    ssh-keygen -t ed25519 -f /ssh-keys/agent-key -N "" -q
    chown jenkins:jenkins /ssh-keys/agent-key /ssh-keys/agent-key.pub
fi

# --- Docker socket permissions ---
if [ -S /var/run/docker.sock ]; then
    DOCKER_SOCK_GID=$(stat -c '%g' /var/run/docker.sock)
    if ! getent group "${DOCKER_SOCK_GID}" >/dev/null; then
        groupadd -g "${DOCKER_SOCK_GID}" docker-host
    fi
    DOCKER_GROUP_NAME=$(getent group "${DOCKER_SOCK_GID}" | cut -d: -f1)
    usermod -aG "${DOCKER_GROUP_NAME}" jenkins
    chmod 666 /var/run/docker.sock
fi

exec gosu jenkins /usr/local/bin/jenkins.sh "$@"
