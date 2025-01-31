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

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.Pager;

import alpine.common.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.dependencytrack.common.HttpClientPool;
import org.json.JSONArray;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class GitLabClient {

    private static final Logger LOGGER = Logger.getLogger(GitLabClient.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final GitLabSyncer syncer;
    private final GitLabApi api;
    private final URL baseURL;

    public GitLabClient(final GitLabSyncer syncer, final URL baseURL, final String gitlabToken) {
        this.syncer = syncer;
        this.baseURL = baseURL;
        this.api = new GitLabApi(baseURL.toString(), gitlabToken);
    }

    public String buildUrl(final String appId, final String state, String redirectUri) {
        try {
            URIBuilder builder = new URIBuilder(redirectUri).setPath("/static/oidc-callback.html");
            redirectUri = builder.build().toString();

            builder = new URIBuilder(this.baseURL.toString())
                    .setPath("/oauth/authorize")
                    .addParameter("client_id", appId)
                    .addParameter("redirect_uri", redirectUri)
                    .addParameter("response_type", "code")
                    .addParameter("state", state)
                    .addParameter("scope", String.join("+", "openid", "profile", "email", "read_api"));

            if (builder.getScheme() == null || builder.getScheme().trim().isEmpty())
                builder.setScheme("https");

            return builder.build().toString();
        } catch (URISyntaxException ex) {
            syncer.handleException(LOGGER, ex);
        }

        return null;
    }

    public void getGitLabGroupClaims(final String token, final String appId, final String state, String redirectUri) {
        LOGGER.debug("Synchronizing Dependency-Track permissions with GitLab instance");

        String url = buildUrl(appId, state, redirectUri);

        HttpGet request = new HttpGet(url);
        request.addHeader("accept", "application/json");
        request.addHeader("Authorization", "Bearer " + token);

        try (CloseableHttpResponse response = HttpClientPool.getClient().execute(request)) {
            StatusLine status = response.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK) {
                LOGGER.debug("Successfully synchronized GitLab permissions");
            } else {
                syncer.handleUnexpectedHttpResponse(LOGGER, url, status.getStatusCode(), status.getReasonPhrase());
            }
        } catch (IOException ex) {
            syncer.handleException(LOGGER, ex);
        }
    }

    // JSONArray to ArrayList simple converter
    public ArrayList<String> jsonToList(final JSONArray jsonArray) {
        ArrayList<String> list = new ArrayList<>();

        for (Object o : jsonArray != null ? jsonArray : Collections.emptyList())
            list.add(o.toString());

        return list;
    }

    public List<Project> getUserProjects() {
        LOGGER.debug("Querying Gitlab for Accessible Projects");

        List<Project> allProjects = null;
        Pager<Project> projectPager;
        ProjectApi pApi = new ProjectApi(api);

        try {
            projectPager = pApi.getProjects(100);
            allProjects = projectPager.all();
        } catch (GitLabApiException e) {
            LOGGER.error("An error occurred while retrieving accessible GitLab projects");
            LOGGER.error(e.toString());
        }

        return allProjects;
    }
}
