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
package org.dependencytrack.tasks.metrics;

import org.dependencytrack.event.ComponentMetricsUpdateEvent;
import org.dependencytrack.model.Analysis;
import org.dependencytrack.model.AnalysisState;
import org.dependencytrack.model.AnalyzerIdentity;
import org.dependencytrack.model.Component;
import org.dependencytrack.model.DependencyMetrics;
import org.dependencytrack.model.Policy;
import org.dependencytrack.model.PolicyViolation;
import org.dependencytrack.model.Project;
import org.dependencytrack.model.Severity;
import org.dependencytrack.model.ViolationAnalysisState;
import org.dependencytrack.model.Vulnerability;
import org.dependencytrack.model.VulnerabilityAlias;
import org.dependencytrack.persistence.jdbi.AnalysisDao;
import org.dependencytrack.persistence.jdbi.MetricsDao;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dependencytrack.model.WorkflowStatus.COMPLETED;
import static org.dependencytrack.model.WorkflowStatus.FAILED;
import static org.dependencytrack.model.WorkflowStep.METRICS_UPDATE;
import static org.dependencytrack.persistence.jdbi.JdbiFactory.withJdbiHandle;


public class ComponentMetricsUpdateTaskTest extends AbstractMetricsUpdateTaskTest {

    @Test
    public void testUpdateCMetricsEmpty() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        // Create risk score configproperties
        createTestConfigProperties();

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        qm.createComponent(component, false);
        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(component.getUuid()));
        final DependencyMetrics metrics = withJdbiHandle(handle -> handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isZero();
        assertThat(metrics.getMedium()).isZero();
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isZero();
        assertThat(metrics.getSuppressed()).isZero();
        assertThat(metrics.getFindingsTotal()).isZero();
        assertThat(metrics.getFindingsAudited()).isZero();
        assertThat(metrics.getFindingsUnaudited()).isZero();
        assertThat(metrics.getInheritedRiskScore()).isZero();
        assertThat(metrics.getPolicyViolationsFail()).isZero();
        assertThat(metrics.getPolicyViolationsWarn()).isZero();
        assertThat(metrics.getPolicyViolationsInfo()).isZero();
        assertThat(metrics.getPolicyViolationsTotal()).isZero();
        assertThat(metrics.getPolicyViolationsAudited()).isZero();
        assertThat(metrics.getPolicyViolationsUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        assertThat(component.getLastInheritedRiskScore()).isZero();
    }

    @Test
    public void testWorkflowStateOnMetricsUpdateFailure() {
        var componentMetricsUpdateEvent = new ComponentMetricsUpdateEvent(UUID.randomUUID());
        qm.createWorkflowSteps(componentMetricsUpdateEvent.getChainIdentifier());
        new ComponentMetricsUpdateTask().inform(componentMetricsUpdateEvent);
        qm.getPersistenceManager().refresh(qm.getWorkflowStateByTokenAndStep(componentMetricsUpdateEvent.getChainIdentifier(), METRICS_UPDATE));
        assertThat(qm.getWorkflowStateByTokenAndStep(componentMetricsUpdateEvent.getChainIdentifier(), METRICS_UPDATE)).satisfies(
                workflowState -> {
                    assertThat(workflowState.getStatus()).isEqualTo(FAILED);
                    assertThat(workflowState.getStartedAt()).isNotNull();
                    assertThat(workflowState.getParent()).isNotNull();
                    assertThat(workflowState.getUpdatedAt()).isBefore(Date.from(Instant.now()));
                    assertThat(workflowState.getFailureReason()).contains("Component with UUID %s does not exist".formatted(componentMetricsUpdateEvent.getUuid()));
                }
        );
    }

    @Test
    public void testUpdateMetricsUnchanged() {

        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        // Create risk score configproperties
        createTestConfigProperties();

        final var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        qm.createComponent(component, false);

        // Record initial project metrics
        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(component.getUuid()));
        final DependencyMetrics metrics = withJdbiHandle(handle -> handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(metrics.getLastOccurrence()).isEqualTo(metrics.getFirstOccurrence());

        // Run the task a second time, without any metric being changed
        final var beforeSecondRun = new Date();
        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(component.getUuid()));

        // Two records should be created in today's partition since it's append-only
        var recentMetrics = withJdbiHandle(handle -> handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(recentMetrics.getLastOccurrence()).isNotEqualTo(metrics.getFirstOccurrence());
        assertThat(recentMetrics.getLastOccurrence()).isAfterOrEqualTo(beforeSecondRun);
    }

    @Test
    public void testUpdateMetricsVulnerabilities() {
        var project = new Project();
        project.setName("acme-app");
        qm.createProject(project, List.of(), false);

        // Create risk score configproperties
        createTestConfigProperties();

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        qm.createComponent(component, false);

        // Create an unaudited vulnerability.
        var vulnUnaudited = new Vulnerability();
        vulnUnaudited.setVulnId("INTERNAL-001");
        vulnUnaudited.setSource(Vulnerability.Source.INTERNAL);
        vulnUnaudited.setSeverity(Severity.HIGH);
        qm.createVulnerability(vulnUnaudited, false);
        qm.addVulnerability(vulnUnaudited, component, AnalyzerIdentity.NONE);

        // Create an audited vulnerability.
        var vulnAudited = new Vulnerability();
        vulnAudited.setVulnId("INTERNAL-002");
        vulnAudited.setSource(Vulnerability.Source.INTERNAL);
        vulnAudited.setSeverity(Severity.MEDIUM);
        qm.createVulnerability(vulnAudited, false);
        qm.addVulnerability(vulnAudited, component, AnalyzerIdentity.NONE);
        withJdbiHandle(handle -> handle.attach(AnalysisDao.class)
                .makeAnalysis(project.getId(), component.getId(), vulnAudited.getId(), AnalysisState.NOT_AFFECTED, null, null, null, false));

        // Create a suppressed vulnerability.
        var vulnSuppressed = new Vulnerability();
        vulnSuppressed.setVulnId("INTERNAL-003");
        vulnSuppressed.setSource(Vulnerability.Source.INTERNAL);
        vulnSuppressed.setSeverity(Severity.MEDIUM);
        qm.createVulnerability(vulnSuppressed, false);
        qm.addVulnerability(vulnSuppressed, component, AnalyzerIdentity.NONE);
        withJdbiHandle(handle -> handle.attach(AnalysisDao.class)
                .makeAnalysis(project.getId(), component.getId(), vulnSuppressed.getId(), AnalysisState.FALSE_POSITIVE, null, null, null, true));

        var componentMetricsUpdateEvent = new ComponentMetricsUpdateEvent(component.getUuid());
        qm.createWorkflowSteps(componentMetricsUpdateEvent.getChainIdentifier());
        new ComponentMetricsUpdateTask().inform(componentMetricsUpdateEvent);

        final DependencyMetrics metrics = withJdbiHandle(handle -> handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isEqualTo(1);
        assertThat(metrics.getMedium()).isEqualTo(1); // One is suppressed
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isEqualTo(2); // One is suppressed
        assertThat(metrics.getSuppressed()).isEqualTo(1);
        assertThat(metrics.getFindingsTotal()).isEqualTo(2); // One is suppressed
        assertThat(metrics.getFindingsAudited()).isEqualTo(1);
        assertThat(metrics.getFindingsUnaudited()).isEqualTo(1);
        assertThat(metrics.getInheritedRiskScore()).isEqualTo(8.0);
        assertThat(metrics.getPolicyViolationsFail()).isZero();
        assertThat(metrics.getPolicyViolationsWarn()).isZero();
        assertThat(metrics.getPolicyViolationsInfo()).isZero();
        assertThat(metrics.getPolicyViolationsTotal()).isZero();
        assertThat(metrics.getPolicyViolationsAudited()).isZero();
        assertThat(metrics.getPolicyViolationsUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();
        qm.getPersistenceManager().refresh(qm.getWorkflowStateByTokenAndStep(componentMetricsUpdateEvent.getChainIdentifier(), METRICS_UPDATE));
        qm.getPersistenceManager().refresh(component);
        assertThat(qm.getWorkflowStateByTokenAndStep(componentMetricsUpdateEvent.getChainIdentifier(), METRICS_UPDATE)).satisfies(
                state -> {
                    assertThat(state.getStartedAt()).isNotNull();
                    assertThat(state.getUpdatedAt()).isNotNull();
                    assertThat(state.getStatus()).isEqualTo(COMPLETED);
                }
        );
        assertThat(component.getLastInheritedRiskScore()).isEqualTo(8.0);
    }


    @Test
    public void testUpdateMetricsVulnerabilitiesWhenSeverityIsOverridden() {
        var project = new Project();
        project.setName("acme-app");
        qm.createProject(project, List.of(), false);

        // Create risk score configproperties
        createTestConfigProperties();

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        qm.createComponent(component, false);

        // Create an unaudited vulnerability.
        var vulnUnaudited = new Vulnerability();
        vulnUnaudited.setVulnId("INTERNAL-001");
        vulnUnaudited.setSource(Vulnerability.Source.INTERNAL);
        vulnUnaudited.setSeverity(Severity.HIGH);
        qm.createVulnerability(vulnUnaudited, false);
        qm.addVulnerability(vulnUnaudited, component, AnalyzerIdentity.NONE);

        // Create an audited vulnerability.
        var vulnAudited = new Vulnerability();
        vulnAudited.setVulnId("INTERNAL-002");
        vulnAudited.setSource(Vulnerability.Source.INTERNAL);
        vulnAudited.setSeverity(Severity.MEDIUM);
        qm.createVulnerability(vulnAudited, false);
        qm.addVulnerability(vulnAudited, component, AnalyzerIdentity.NONE);
        withJdbiHandle(handle -> handle.attach(AnalysisDao.class)
                .makeAnalysis(project.getId(), component.getId(), vulnAudited.getId(), AnalysisState.NOT_AFFECTED, null, null, null, false));

        // Create an overridden vulnerability.
        var vulnSuppressed = new Vulnerability();
        vulnSuppressed.setVulnId("INTERNAL-003");
        vulnSuppressed.setSource(Vulnerability.Source.INTERNAL);
        vulnSuppressed.setSeverity(Severity.MEDIUM);
        qm.createVulnerability(vulnSuppressed, false);
        qm.addVulnerability(vulnSuppressed, component, AnalyzerIdentity.NONE);
        var analysis = new Analysis();
        analysis.setComponent(component);
        analysis.setSeverity(Severity.LOW);
        analysis.setAnalysisState(AnalysisState.FALSE_POSITIVE);
        analysis.setVulnerability(vulnSuppressed);
        qm.persist(analysis);
        var componentMetricsUpdateEvent = new ComponentMetricsUpdateEvent(component.getUuid());
        qm.createWorkflowSteps(componentMetricsUpdateEvent.getChainIdentifier());
        new ComponentMetricsUpdateTask().inform(componentMetricsUpdateEvent);

        final DependencyMetrics metrics = withJdbiHandle(handle ->
                handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isEqualTo(1);
        assertThat(metrics.getMedium()).isEqualTo(1);
        assertThat(metrics.getLow()).isEqualTo(1); //One is overridden with low
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isEqualTo(3); // One is overridden vulnerability
        assertThat(metrics.getSuppressed()).isEqualTo(0); // vulnerability is overridden but not suppressed
        assertThat(metrics.getFindingsTotal()).isEqualTo(3); // One is overridden vulnerability
        assertThat(metrics.getFindingsAudited()).isEqualTo(2); // Overridden vulnerability is also audited
        assertThat(metrics.getFindingsUnaudited()).isEqualTo(1);
        assertThat(metrics.getInheritedRiskScore()).isEqualTo(9.0);
        assertThat(metrics.getPolicyViolationsFail()).isZero();
        assertThat(metrics.getPolicyViolationsWarn()).isZero();
        assertThat(metrics.getPolicyViolationsInfo()).isZero();
        assertThat(metrics.getPolicyViolationsTotal()).isZero();
        assertThat(metrics.getPolicyViolationsAudited()).isZero();
        assertThat(metrics.getPolicyViolationsUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        qm.getPersistenceManager().refresh(qm.getWorkflowStateByTokenAndStep(componentMetricsUpdateEvent.getChainIdentifier(), METRICS_UPDATE));
        assertThat(qm.getWorkflowStateByTokenAndStep(componentMetricsUpdateEvent.getChainIdentifier(), METRICS_UPDATE)).satisfies(
                state -> {
                    assertThat(state.getStartedAt()).isNotNull();
                    assertThat(state.getUpdatedAt()).isNotNull();
                    assertThat(state.getStatus()).isEqualTo(COMPLETED);
                }
        );
        assertThat(component.getLastInheritedRiskScore()).isEqualTo(9.0);
    }

    @Test
    public void testUpdateMetricsPolicyViolations() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        // Create risk score configproperties
        createTestConfigProperties();

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        qm.createComponent(component, false);

        // Create an unaudited violation.
        createPolicyViolation(component, Policy.ViolationState.FAIL, PolicyViolation.Type.LICENSE);

        // Create an audited violation.
        final PolicyViolation auditedViolation = createPolicyViolation(component, Policy.ViolationState.WARN, PolicyViolation.Type.OPERATIONAL);
        qm.makeViolationAnalysis(component, auditedViolation, ViolationAnalysisState.APPROVED, false);

        // Create a suppressed violation.
        final PolicyViolation suppressedViolation = createPolicyViolation(component, Policy.ViolationState.INFO, PolicyViolation.Type.SECURITY);
        qm.makeViolationAnalysis(component, suppressedViolation, ViolationAnalysisState.REJECTED, true);

        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(component.getUuid()));
        final DependencyMetrics metrics = withJdbiHandle(handle ->
                handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isZero();
        assertThat(metrics.getMedium()).isZero();
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isZero();
        assertThat(metrics.getSuppressed()).isZero();
        assertThat(metrics.getFindingsTotal()).isZero();
        assertThat(metrics.getFindingsAudited()).isZero();
        assertThat(metrics.getFindingsUnaudited()).isZero();
        assertThat(metrics.getInheritedRiskScore()).isZero();
        assertThat(metrics.getPolicyViolationsFail()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsWarn()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsInfo()).isZero(); // Suppressed
        assertThat(metrics.getPolicyViolationsTotal()).isEqualTo(2);
        assertThat(metrics.getPolicyViolationsAudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsUnaudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero(); // Suppressed
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isEqualTo(1);
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        assertThat(component.getLastInheritedRiskScore()).isZero();
    }

    @Test
    public void testUpdateMetricsWithDuplicateAliases() {
        var project = new Project();
        project.setName("acme-app");
        project = qm.createProject(project, List.of(), false);

        // Create risk score configproperties
        createTestConfigProperties();

        var component = new Component();
        component.setProject(project);
        component.setName("acme-lib");
        qm.createComponent(component, false);

        // Create four distinct vulnerabilities:
        //   A: INTERNAL -> INTERNAL-001
        //   B: GITHUB   -> GHSA-002
        //   C: OSSINDEX -> SONATYPE-003
        //   D: VULNDB   -> VULNDB-004
        var vulnA = new Vulnerability();
        vulnA.setVulnId("INTERNAL-001");
        vulnA.setSource(Vulnerability.Source.INTERNAL);
        vulnA.setSeverity(Severity.HIGH);
        vulnA = qm.createVulnerability(vulnA, false);
        qm.addVulnerability(vulnA, component, AnalyzerIdentity.NONE);

        var vulnB = new Vulnerability();
        vulnB.setVulnId("GHSA-002");
        vulnB.setSource(Vulnerability.Source.GITHUB);
        vulnB.setSeverity(Severity.MEDIUM);
        vulnB = qm.createVulnerability(vulnB, false);
        qm.addVulnerability(vulnB, component, AnalyzerIdentity.NONE);

        var vulnC = new Vulnerability();
        vulnC.setVulnId("SONATYPE-003");
        vulnC.setSource(Vulnerability.Source.OSSINDEX);
        vulnC.setSeverity(Severity.MEDIUM);
        vulnC = qm.createVulnerability(vulnC, false);
        qm.addVulnerability(vulnC, component, AnalyzerIdentity.NONE);

        var vulnD = new Vulnerability();
        vulnD.setVulnId("VULNDB-004");
        vulnD.setSource(Vulnerability.Source.VULNDB);
        vulnD.setSeverity(Severity.LOW);
        vulnD = qm.createVulnerability(vulnD, false);
        qm.addVulnerability(vulnD, component, AnalyzerIdentity.NONE);

        // Make A and alias of C
        final var aliasAtoC = new VulnerabilityAlias();
        aliasAtoC.setInternalId(vulnA.getVulnId());
        aliasAtoC.setSonatypeId(vulnC.getVulnId());
        qm.persist(aliasAtoC);

        // Make A also an alias of D
        final var aliasAtoD = new VulnerabilityAlias();
        aliasAtoD.setInternalId(vulnA.getVulnId());
        aliasAtoD.setVulnDbId(vulnD.getVulnId());
        qm.persist(aliasAtoD);

        // Kick off metrics calculation.
        // Expectation is that both C and D will not be considered because they alias A.

        new ComponentMetricsUpdateTask().inform(new ComponentMetricsUpdateEvent(component.getUuid()));
        final DependencyMetrics metrics = withJdbiHandle(handle -> handle.attach(MetricsDao.class).getMostRecentDependencyMetrics(component.getId()));
        assertThat(metrics.getCritical()).isZero();
        assertThat(metrics.getHigh()).isEqualTo(1); // INTERNAL-001
        assertThat(metrics.getMedium()).isEqualTo(1); // GHSA-002
        assertThat(metrics.getLow()).isZero();
        assertThat(metrics.getUnassigned()).isZero();
        assertThat(metrics.getVulnerabilities()).isEqualTo(2);
        assertThat(metrics.getSuppressed()).isEqualTo(0);
        assertThat(metrics.getFindingsTotal()).isEqualTo(2);
        assertThat(metrics.getFindingsAudited()).isEqualTo(0);
        assertThat(metrics.getFindingsUnaudited()).isEqualTo(2);
        assertThat(metrics.getInheritedRiskScore()).isEqualTo(8.0);
        assertThat(metrics.getPolicyViolationsFail()).isZero();
        assertThat(metrics.getPolicyViolationsWarn()).isZero();
        assertThat(metrics.getPolicyViolationsInfo()).isZero();
        assertThat(metrics.getPolicyViolationsTotal()).isZero();
        assertThat(metrics.getPolicyViolationsAudited()).isZero();
        assertThat(metrics.getPolicyViolationsUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityTotal()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityAudited()).isZero();
        assertThat(metrics.getPolicyViolationsSecurityUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseTotal()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseAudited()).isZero();
        assertThat(metrics.getPolicyViolationsLicenseUnaudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalTotal()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalAudited()).isZero();
        assertThat(metrics.getPolicyViolationsOperationalUnaudited()).isZero();

        qm.getPersistenceManager().refresh(component);
        assertThat(component.getLastInheritedRiskScore()).isEqualTo(8.0);
    }

}