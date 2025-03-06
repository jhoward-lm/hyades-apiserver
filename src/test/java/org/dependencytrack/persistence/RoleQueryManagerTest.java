/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) OWASP Foundation. All Rights Reserved.
 */
package org.dependencytrack.persistence;

import org.dependencytrack.model.Project;
import org.dependencytrack.model.Role;
import org.dependencytrack.model.MappedRole;

import alpine.Config;
import alpine.model.ManagedUser;
import alpine.model.Permission;

import java.util.Arrays;
import java.util.List;

import org.dependencytrack.PersistenceCapableTest;
import org.dependencytrack.event.kafka.KafkaProducerInitializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class RoleQueryManagerTest extends PersistenceCapableTest {

    @BeforeClass
    public static void beforeClass() {
        Config.enableUnitTests();
    }

    @AfterClass
    public static void afterClass() {
        KafkaProducerInitializer.tearDown();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Test
    public void testGetUserProjectPermissions() {
        final var testProject = new Project();
        testProject.setId(1);
        testProject.setName("test-project");
        testProject.setVersion("1.0.0");
        qm.persist(testProject);

        final var readPermission = new Permission();
        readPermission.setId(1);
        readPermission.setName("read");
        readPermission.setDescription("permission to read");

        final var writePermission = new Permission();
        writePermission.setId(2);
        writePermission.setName("write");
        writePermission.setDescription("permission to write");

        List<Permission> expectedPermissions = Arrays.asList(
                readPermission,
                writePermission);

        final var testUser = new ManagedUser();
        testUser.setFullname("test user created for testing");
        testUser.setId(1);
        testUser.setUsername("test-user");

        final var maintainerRole = new Role();
        maintainerRole.setId(1);
        maintainerRole.setName("maintainer");
        maintainerRole.setPermissions(expectedPermissions);

        final var mappedRole = new MappedRole();
        mappedRole.setId(1);
        mappedRole.setProject(testProject);
        mappedRole.setManagedUsers(Arrays.asList(testUser));
        mappedRole.setRole(maintainerRole);

        List<Permission> actualPermissions = qm.getUserProjectPermissions("", "");

        Assert.assertEquals(actualPermissions, expectedPermissions);
    }

}
