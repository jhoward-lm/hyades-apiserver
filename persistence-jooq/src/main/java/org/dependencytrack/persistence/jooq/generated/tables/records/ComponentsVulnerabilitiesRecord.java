/*
 * This file is generated by jOOQ.
 */
package org.dependencytrack.persistence.jooq.generated.tables.records;


import org.dependencytrack.persistence.jooq.generated.tables.ComponentsVulnerabilities;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ComponentsVulnerabilitiesRecord extends TableRecordImpl<ComponentsVulnerabilitiesRecord> {

    private static final long serialVersionUID = -726127357;

    /**
     * Setter for <code>COMPONENTS_VULNERABILITIES.COMPONENT_ID</code>.
     */
    public ComponentsVulnerabilitiesRecord setComponentId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>COMPONENTS_VULNERABILITIES.COMPONENT_ID</code>.
     */
    public Long getComponentId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>COMPONENTS_VULNERABILITIES.VULNERABILITY_ID</code>.
     */
    public ComponentsVulnerabilitiesRecord setVulnerabilityId(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>COMPONENTS_VULNERABILITIES.VULNERABILITY_ID</code>.
     */
    public Long getVulnerabilityId() {
        return (Long) get(1);
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ComponentsVulnerabilitiesRecord
     */
    public ComponentsVulnerabilitiesRecord() {
        super(ComponentsVulnerabilities.COMPONENTS_VULNERABILITIES);
    }

    /**
     * Create a detached, initialised ComponentsVulnerabilitiesRecord
     */
    public ComponentsVulnerabilitiesRecord(Long componentId, Long vulnerabilityId) {
        super(ComponentsVulnerabilities.COMPONENTS_VULNERABILITIES);

        setComponentId(componentId);
        setVulnerabilityId(vulnerabilityId);
        resetTouchedOnNotNull();
    }
}
