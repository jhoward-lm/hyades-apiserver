/*
 * This file is generated by jOOQ.
 */
package org.dependencytrack.persistence.jooq.generated.routines;


import org.dependencytrack.persistence.jooq.generated.DefaultSchema;
import org.jooq.impl.AbstractRoutine;
import org.jooq.impl.DSL;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class UpdatePortfolioMetrics extends AbstractRoutine<java.lang.Void> {

    private static final long serialVersionUID = 1145470713;

    /**
     * Create a new routine call instance
     */
    public UpdatePortfolioMetrics() {
        super("UPDATE_PORTFOLIO_METRICS", DefaultSchema.DEFAULT_SCHEMA, DSL.comment(""));
        setSQLUsable(false);
    }
}
