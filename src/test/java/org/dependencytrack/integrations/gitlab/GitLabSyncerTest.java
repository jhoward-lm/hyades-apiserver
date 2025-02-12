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
package org.dependencytrack.integrations.gitlab;

import org.junit.Assert;
import alpine.Config;
import alpine.model.ConfigProperty;
import alpine.model.OidcUser;

import org.dependencytrack.PersistenceCapableTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;

/**
 * This test suite validates the integration with the GitLab API.
 */
@RunWith(MockitoJUnitRunner.class)
public class GitLabSyncerTest extends PersistenceCapableTest {

    @Mock
    private Config config;

    @Mock
    private ConfigProperty configProperty;

    @Mock
    private OidcUser user;

    @Mock
    private GitLabClient gitLabClient;

    @InjectMocks
    private GitLabSyncer gitLabSyncer;

    @Before
    public void setup() {
        // Mocking the config instance
        when(configProperty.getPropertyValue()).thenReturn("true");
        when(gitLabClient.getGitLabProjects())
                .thenReturn(Arrays.asList(new GitLabProject("project1", "this/test/project1", null),
                        new GitLabProject("project2", "this/test/project2", null)));
    }

    /**
     * Validates that the integration metadata is correctly defined.
     */
    @Test
    public void testIntegrationMetadata() {
        Assert.assertEquals("GitLab", gitLabSyncer.name());
        Assert.assertEquals("Synchronizes user permissions from connected GitLab instance", gitLabSyncer.description());
    }

    /**
     * Validates that the integration is enabled when the GITLAB_ENABLED property is
     * set to true.
     */
    @Test
    public void testIsEnabled() {
        when(configProperty.getPropertyValue()).thenReturn("true");
        Assert.assertTrue(gitLabSyncer.isEnabled());
    }

    /**
     * Validates that the integration is disabled when the GITLAB_ENABLED property
     * is set to false.
     */
    @Test
    public void testIsDisabled() {
        when(configProperty.getPropertyValue()).thenReturn("false");
        Assert.assertFalse(gitLabSyncer.isEnabled());
    }

    /**
     * Validates that the synchronize method is correctly executed when the
     * integration is enabled.
     */
    @Test
    public void testSynchronize() {
        gitLabSyncer.synchronize();
        verify(gitLabClient).getGitLabProjects();
        verify(qm, times(2)).createProject(anyString(), any(), any(), any(), any(), any(), any(), anyBoolean());
        verify(qm, times(2)).getTeam(anyString());
        verify(qm, times(2)).createTeam(anyString());
    }
}