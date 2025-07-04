/*
 * This file is generated by jOOQ.
 */
package org.dependencytrack.persistence.jooq.generated.tables.records;


import org.dependencytrack.persistence.jooq.generated.tables.NotificationRuleTeams;
import org.jooq.Record2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class NotificationRuleTeamsRecord extends UpdatableRecordImpl<NotificationRuleTeamsRecord> {

    private static final long serialVersionUID = 1761880662;

    /**
     * Setter for <code>NOTIFICATIONRULE_TEAMS.NOTIFICATIONRULE_ID</code>.
     */
    public NotificationRuleTeamsRecord setNotificationruleId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>NOTIFICATIONRULE_TEAMS.NOTIFICATIONRULE_ID</code>.
     */
    public Long getNotificationruleId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>NOTIFICATIONRULE_TEAMS.TEAM_ID</code>.
     */
    public NotificationRuleTeamsRecord setTeamId(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>NOTIFICATIONRULE_TEAMS.TEAM_ID</code>.
     */
    public Long getTeamId() {
        return (Long) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Long, Long> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached NotificationRuleTeamsRecord
     */
    public NotificationRuleTeamsRecord() {
        super(NotificationRuleTeams.NOTIFICATIONRULE_TEAMS);
    }

    /**
     * Create a detached, initialised NotificationRuleTeamsRecord
     */
    public NotificationRuleTeamsRecord(Long notificationruleId, Long teamId) {
        super(NotificationRuleTeams.NOTIFICATIONRULE_TEAMS);

        setNotificationruleId(notificationruleId);
        setTeamId(teamId);
        resetTouchedOnNotNull();
    }
}
