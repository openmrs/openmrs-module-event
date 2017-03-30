package org.openmrs.event.api.db.hibernate;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.OpenmrsProfile;

import org.openmrs.event.HibernateEventInterceptorSuperClass;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes in a single hibernate session and fires the necessary events. Any changes/inserts/deletes
 * made to the DB that are not made through the application won't be detected by the module.
 *
 * We use a Stack here to handle any nested transactions that may occur within a single thread
 */
@OpenmrsProfile(openmrsPlatformVersion = "1.*")
public class HibernateEventInterceptor_1_x extends HibernateEventInterceptorSuperClass {
	protected final Log log = LogFactory.getLog(HibernateEventInterceptor_1_x.class);

}
