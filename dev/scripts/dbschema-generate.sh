#!/usr/bin/env bash

# This file is part of Dependency-Track.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# Copyright (c) OWASP Foundation. All Rights Reserved.

set -euo pipefail

SCRIPT_DIR=$(cd -P -- "$(dirname "${0}")" && pwd -P)
ROOT_DIR=$(cd -P -- "${SCRIPT_DIR}/../../" && pwd -P)

# shellcheck disable=SC1091
[[ -f $HOME/.bash_aliases ]] && source "${HOME}/.bash_aliases"

# Enable support for aliases named "docker"
shopt -s expand_aliases

CONTAINER_ID=$(
  docker run --detach --rm \
    --env POSTGRES_DB=dtrack \
    --env POSTGRES_USER=dtrack \
    --env POSTGRES_PASSWORD=dtrack \
    --publish 5432 \
    postgres:13-alpine
)

CONTAINER_PORT=$(
  docker inspect \
    --format '{{$port := index .NetworkSettings.Ports "5432/tcp"}}{{(index $port 0).HostPort}}' \
    "${CONTAINER_ID}"
)

TMP_LIQUIBASE_CONFIG_FILE=$(mktemp -p "${ROOT_DIR}")
LIQUIBASE_CONFIG=(
  "changeLogFile=migration/changelog-main.xml"
  "url=jdbc:postgresql://localhost:${CONTAINER_PORT}/dtrack"
  "username=dtrack"
  "password=dtrack"
)

printf "%s\n" "${LIQUIBASE_CONFIG[@]}" > "${TMP_LIQUIBASE_CONFIG_FILE}"

if mvn liquibase:update \
  --activate-profiles enhance \
  --define liquibase.analytics.enabled=false \
  --define liquibase.propertyFile="$(basename "${TMP_LIQUIBASE_CONFIG_FILE}")"; then
  docker exec "${CONTAINER_ID}" pg_dump -Udtrack --schema-only --no-owner --no-privileges dtrack |
    sed -e '/^--/d' |
    cat -s > "${ROOT_DIR}/schema.sql"
fi

docker stop "${CONTAINER_ID}"

rm "${TMP_LIQUIBASE_CONFIG_FILE}"
