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

import alpine.model.Team;
import alpine.notification.NotificationLevel;
import alpine.persistence.PaginatedResult;
import alpine.resources.AlpineRequest;
import org.dependencytrack.model.NotificationPublisher;
import org.dependencytrack.model.NotificationRule;
import org.dependencytrack.model.Project;
import org.dependencytrack.model.Tag;
import org.dependencytrack.notification.NotificationScope;
import org.dependencytrack.notification.publisher.PublisherClass;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.dependencytrack.util.PersistenceUtil.assertPersistent;
import static org.dependencytrack.util.PersistenceUtil.assertPersistentAll;

public class NotificationQueryManager extends QueryManager implements IQueryManager {


    /**
     * Constructs a new QueryManager.
     * @param pm a PersistenceManager object
     */
    NotificationQueryManager(final PersistenceManager pm) {
        super(pm);
    }

    /**
     * Constructs a new QueryManager.
     * @param pm a PersistenceManager object
     * @param request an AlpineRequest object
     */
    NotificationQueryManager(final PersistenceManager pm, final AlpineRequest request) {
        super(pm, request);
    }

    /**
     * Creates a new NotificationRule.
     * @param name the name of the rule
     * @param scope the scope
     * @param level the level
     * @param publisher the publisher
     * @return a new NotificationRule
     */
    public NotificationRule createNotificationRule(String name, NotificationScope scope, NotificationLevel level, NotificationPublisher publisher) {
        return callInTransaction(() -> {
            final NotificationRule rule = new NotificationRule();
            rule.setName(name);
            rule.setScope(scope);
            rule.setNotificationLevel(level);
            rule.setPublisher(publisher);
            rule.setEnabled(true);
            rule.setNotifyChildren(true);
            rule.setLogSuccessfulPublish(false);
            return persist(rule);
        });
    }

    /**
     * Updated an existing NotificationRule.
     * @param transientRule the rule to update
     * @return a NotificationRule
     */
    public NotificationRule updateNotificationRule(NotificationRule transientRule) {
        return callInTransaction(() -> {
            final NotificationRule rule = getObjectByUuid(NotificationRule.class, transientRule.getUuid());
            rule.setName(transientRule.getName());
            rule.setEnabled(transientRule.isEnabled());
            rule.setNotifyChildren(transientRule.isNotifyChildren());
            rule.setLogSuccessfulPublish(transientRule.isLogSuccessfulPublish());
            rule.setNotificationLevel(transientRule.getNotificationLevel());
            rule.setPublisherConfig(transientRule.getPublisherConfig());
            rule.setNotifyOn(transientRule.getNotifyOn());
            bind(rule, resolveTags(transientRule.getTags()));
            return persist(rule);
        });
    }

    /**
     * Returns a paginated list of all notification rules.
     * @return a paginated list of NotificationRules
     */
    public PaginatedResult getNotificationRules() {
        final Query<NotificationRule> query = pm.newQuery(NotificationRule.class);
        if (orderBy == null) {
            query.setOrdering("name asc");
        }
        if (filter != null) {
            query.setFilter("name.toLowerCase().matches(:name) || publisher.name.toLowerCase().matches(:name)");
            final String filterString = ".*" + filter.toLowerCase() + ".*";
            return execute(query, filterString);
        }
        return execute(query);
    }

    /**
     * Retrieves all NotificationPublishers.
     * This method if designed NOT to provide paginated results.
     * @return list of all NotificationPublisher objects
     */
    @SuppressWarnings("unchecked")
    public List<NotificationPublisher> getAllNotificationPublishers() {
        final Query<NotificationPublisher> query = pm.newQuery(NotificationPublisher.class);
        query.getFetchPlan().addGroup(NotificationPublisher.FetchGroup.ALL.name());
        query.setOrdering("name asc");
        return (List<NotificationPublisher>)query.execute();
    }

    /**
     * Retrieves a NotificationPublisher by its name.
     * @param name The name of the NotificationPublisher
     * @return a NotificationPublisher
     */
    public NotificationPublisher getNotificationPublisher(final String name) {
        final Query<NotificationPublisher> query = pm.newQuery(NotificationPublisher.class, "name == :name");
        query.setRange(0, 1);
        return singleResult(query.execute(name));
    }

    /**
     * Retrieves a NotificationPublisher by its class.
     * @param clazz The Class of the NotificationPublisher
     * @return a NotificationPublisher
     */
    public NotificationPublisher getDefaultNotificationPublisher(final PublisherClass clazz) {
        return getDefaultNotificationPublisher(clazz.name());
    }

    /**
     * Retrieves a NotificationPublisher by its class.
     * @param clazz The Class of the NotificationPublisher
     * @return a NotificationPublisher
     */
    private NotificationPublisher getDefaultNotificationPublisher(final String clazz) {
        final Query<NotificationPublisher> query = pm.newQuery(NotificationPublisher.class, "publisherClass == :publisherClass && defaultPublisher == true");
        query.getFetchPlan().addGroup(NotificationPublisher.FetchGroup.ALL.name());
        query.setRange(0, 1);
        return singleResult(query.execute(clazz));
    }

    /**
     * Retrieves a DefaultNotificationPublisher by its name.
     * @param name The name of the DefaultNotificationPublisher
     * @return a DefaultNotificationPublisher
     */
    public NotificationPublisher getDefaultNotificationPublisherByName(final String name) {
        final Query<NotificationPublisher> query = pm.newQuery(NotificationPublisher.class, "name == :name && defaultPublisher == true");
        query.getFetchPlan().addGroup(NotificationPublisher.FetchGroup.ALL.name());
        query.setRange(0, 1);
        return singleResult(query.execute(name));
    }

    /**
     * Creates a NotificationPublisher object.
     * @param name The name of the NotificationPublisher
     * @return a NotificationPublisher
     */
    public NotificationPublisher createNotificationPublisher(final String name, final String description,
                                                             final String publisherClass, final String templateContent,
                                                             final String templateMimeType, final boolean defaultPublisher) {
        return callInTransaction(() -> {
            final NotificationPublisher publisher = new NotificationPublisher();
            publisher.setName(name);
            publisher.setDescription(description);
            publisher.setPublisherClass(publisherClass);
            publisher.setTemplate(templateContent);
            publisher.setTemplateMimeType(templateMimeType);
            publisher.setDefaultPublisher(defaultPublisher);
            return pm.makePersistent(publisher);
        });
    }

    /**
     * Updates a NotificationPublisher.
     * @return a NotificationPublisher object
     */
    public NotificationPublisher updateNotificationPublisher(NotificationPublisher transientPublisher) {
        NotificationPublisher publisher = null;
        if (transientPublisher.getId() > 0) {
            publisher = getObjectById(NotificationPublisher.class, transientPublisher.getId());
        } else if (transientPublisher.isDefaultPublisher()) {
            publisher = getDefaultNotificationPublisher(transientPublisher.getPublisherClass());
        }
        if (publisher != null) {
            publisher.setName(transientPublisher.getName());
            publisher.setDescription(transientPublisher.getDescription());
            publisher.setPublisherClass(transientPublisher.getPublisherClass());
            publisher.setTemplate(transientPublisher.getTemplate());
            publisher.setTemplateMimeType(transientPublisher.getTemplateMimeType());
            publisher.setDefaultPublisher(transientPublisher.isDefaultPublisher());
            return persist(publisher);
        }
        return null;
    }

    /**
     * Removes projects from NotificationRules
     */
    @SuppressWarnings("unchecked")
    public void removeProjectFromNotificationRules(final Project project) {
        final Query<NotificationRule> query = pm.newQuery(NotificationRule.class, "projects.contains(:project)");
        try {
            for (final NotificationRule rule : (List<NotificationRule>) query.execute(project)) {
                rule.getProjects().remove(project);
                if (!pm.currentTransaction().isActive()) {
                    persist(rule);
                }
            }
        } finally {
            query.closeAll();
        }
    }

    /**
     * Removes teams from NotificationRules
     */
    @SuppressWarnings("unchecked")
    public void removeTeamFromNotificationRules(final Team team) {
        final Query<NotificationRule> query = pm.newQuery(NotificationRule.class, "teams.contains(:team)");
        for (final NotificationRule rule: (List<NotificationRule>) query.execute(team)) {
            rule.getTeams().remove(team);
            persist(rule);
        }
    }

    /**
     * Delete a notification publisher and associated rules.
     */
    public void deleteNotificationPublisher(final NotificationPublisher notificationPublisher) {
        final Query<NotificationRule> query = pm.newQuery(NotificationRule.class, "publisher.uuid == :uuid");
        query.deletePersistentAll(notificationPublisher.getUuid());
        delete(notificationPublisher);
    }

    /**
     * @since 4.12.3
     */
    @Override
    public boolean bind(final NotificationRule notificationRule, final Collection<Tag> tags, final boolean keepExisting) {
        assertPersistent(notificationRule, "notificationRule must be persistent");
        assertPersistentAll(tags, "tags must be persistent");

        return callInTransaction(() -> {
            boolean modified = false;

            if (!keepExisting) {
                final Iterator<Tag> existingTagsIterator = notificationRule.getTags().iterator();
                while (existingTagsIterator.hasNext()) {
                    final Tag existingTag = existingTagsIterator.next();
                    if (!tags.contains(existingTag)) {
                        existingTagsIterator.remove();
                        existingTag.getNotificationRules().remove(notificationRule);
                        modified = true;
                    }
                }
            }
            for (final Tag tag : tags) {
                if (!notificationRule.getTags().contains(tag)) {
                    notificationRule.getTags().add(tag);

                    if (tag.getNotificationRules() == null) {
                        tag.setNotificationRules(new HashSet<>(Set.of(notificationRule)));
                    } else {
                        tag.getNotificationRules().add(notificationRule);
                    }

                    modified = true;
                }
            }
            return modified;
        });
    }

    /**
     * @since 4.12.0
     */
    @Override
    public boolean bind(final NotificationRule notificationRule, final Collection<Tag> tags) {
        return bind(notificationRule, tags, /* keepExisting */ false);
    }
}
