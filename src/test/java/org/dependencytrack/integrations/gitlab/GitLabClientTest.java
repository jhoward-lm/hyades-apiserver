package org.dependencytrack.integrations.gitlab;

import alpine.Config;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.dependencytrack.event.kafka.KafkaProducerInitializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class GitLabClientTest {

        @BeforeClass
        public static void beforeClass() {
                Config.enableUnitTests();
        }

        @AfterClass
        public static void after() {
                KafkaProducerInitializer.tearDown();
        }

        @Rule
        public WireMockRule wireMockRule = new WireMockRule();

        @Test
        public void testGetGitLabProjects() throws URISyntaxException, IOException {
                String accessToken = "I_AM_AN_ACCESS_TOKEN";

                String result = Files
                                .readString(Paths.get(
                                                "src/test/java/org/dependencytrack/integrations/gitlab/ResponseData_1.json"));
                String result2 = Files
                                .readString(Paths.get(
                                                "src/test/java/org/dependencytrack/integrations/gitlab/ResponseData_2.json"));

                WireMock.stubFor(WireMock.post("/api/graphql")
                                .inScenario("test-get-gitlab-projects")
                                .whenScenarioStateIs(Scenario.STARTED)
                                .willReturn(WireMock.ok().withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                                .withBody(result))
                                .willSetStateTo("second-page"));

                WireMock.stubFor(WireMock.post("/api/graphql")
                                .inScenario("test-get-gitlab-projects")
                                .whenScenarioStateIs("second-page")
                                .willReturn(WireMock.ok().withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                                .withBody(result2))
                                .willSetStateTo("Finished"));

                final var configMock = mock(Config.class);

                when(configMock.getProperty(eq(Config.AlpineKey.OIDC_ISSUER))).thenReturn(wireMockRule.baseUrl());

                GitLabClient gitLabClient = new GitLabClient(accessToken, configMock);

                List<GitLabProject> gitLabProjects = gitLabClient.getGitLabProjects();

                List<String> actualProjectPaths = new ArrayList<String>();
                for (GitLabProject project : gitLabProjects) {
                        actualProjectPaths.add(project.getFullPath());
                }

                List<String> expectedProjectPaths = Arrays.asList(
                                "testing/dummy_1",
                                "still_testing/dummy_2",
                                "guess_what_still_testing/dummy_3",
                                "last_one/dummy_4");

                Assert.assertEquals(actualProjectPaths, expectedProjectPaths);
        }
}