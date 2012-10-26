package org.openmrs.event.api.db.hibernate;

import java.io.Serializable;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.type.Type;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.event.Event;
import org.openmrs.event.Event.Action;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes in a single hibernate session and fires the necessary events. Any changes/inserts/deletes
 * made to the DB that are not made through the application won't be detected by the module.
 */
public class HibernateEventInterceptor extends EmptyInterceptor {
	
	private static final long serialVersionUID = 1L;
	
	protected final Log log = LogFactory.getLog(HibernateEventInterceptor.class);
	
	private ThreadLocal<HashSet<OpenmrsObject>> inserts = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> updates = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> deletes = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> retiredObjects = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> unretiredObjects = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> voidedObjects = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	private ThreadLocal<HashSet<OpenmrsObject>> unvoidedObjects = new ThreadLocal<HashSet<OpenmrsObject>>();
	
	/**
	 * @see org.hibernate.EmptyInterceptor#afterTransactionBegin(org.hibernate.Transaction)
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
		inserts.set(new HashSet<OpenmrsObject>());
		updates.set(new HashSet<OpenmrsObject>());
		deletes.set(new HashSet<OpenmrsObject>());
		retiredObjects.set(new HashSet<OpenmrsObject>());
		unretiredObjects.set(new HashSet<OpenmrsObject>());
		voidedObjects.set(new HashSet<OpenmrsObject>());
		unvoidedObjects.set(new HashSet<OpenmrsObject>());
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof OpenmrsObject) {
			inserts.get().add((OpenmrsObject) entity);
		}
		
		//tells hibernate that there are no changes made here that 
		//need to be propagated to the persistent object and DB
		return false;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
	                            String[] propertyNames, Type[] types) {
		
		if (entity instanceof OpenmrsObject) {
			OpenmrsObject object = (OpenmrsObject) entity;
			updates.get().add(object);
			//Fire events for retired/unretired and voided/unvoided objects
			if (entity instanceof Retireable || entity instanceof Voidable) {
				for (int i = 0; i < propertyNames.length; i++) {
					String auditableProperty = (entity instanceof Retireable) ? "retired" : "voided";
					if (auditableProperty.equals(propertyNames[i])) {
						boolean previousValue = false;
						if (previousState != null && previousState[i] != null)
							previousValue = Boolean.valueOf(previousState[i].toString());
						
						boolean currentValue = false;
						if (currentState != null && currentState[i] != null)
							currentValue = Boolean.valueOf(currentState[i].toString());
						
						if (previousValue != currentValue) {
							if ("retired".equals(auditableProperty)) {
								if (previousValue)
									unretiredObjects.get().add(object);
								else
									retiredObjects.get().add(object);
							} else {
								if (previousValue)
									unvoidedObjects.get().add(object);
								else
									voidedObjects.get().add(object);
							}
						}
						
						break;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onDelete(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof OpenmrsObject) {
			deletes.get().add((OpenmrsObject) entity);
		}
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onCollectionUpdate(java.lang.Object,
	 *      java.io.Serializable)
	 */
	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		if (collection != null) {
			//If a collection element has been added/removed, fire an update event for the parent entity
			Object owningObject = ((PersistentCollection) collection).getOwner();
			if (owningObject instanceof OpenmrsObject) {
				updates.get().add((OpenmrsObject) owningObject);
			}
		}
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#afterTransactionCompletion(org.hibernate.Transaction)
	 */
	@Override
	public void afterTransactionCompletion(Transaction tx) {
		
		try {
			if (tx.wasCommitted()) {
				for (OpenmrsObject delete : deletes.get()) {
					Event.fireAction(Action.PURGED.name(), delete);
				}
				for (OpenmrsObject insert : inserts.get()) {
					Event.fireAction(Action.CREATED.name(), insert);
				}
				for (OpenmrsObject update : updates.get()) {
					Event.fireAction(Action.UPDATED.name(), update);
				}
				for (OpenmrsObject retired : retiredObjects.get()) {
					Event.fireAction(Action.RETIRED.name(), retired);
				}
				for (OpenmrsObject unretired : unretiredObjects.get()) {
					Event.fireAction(Action.UNRETIRED.name(), unretired);
				}
				for (OpenmrsObject voided : voidedObjects.get()) {
					Event.fireAction(Action.VOIDED.name(), voided);
				}
				for (OpenmrsObject unvoided : unvoidedObjects.get()) {
					Event.fireAction(Action.UNVOIDED.name(), unvoided);
				}
			}
		}
		finally {
			//cleanup
			inserts.remove();
			updates.remove();
			deletes.remove();
			retiredObjects.remove();
			unretiredObjects.remove();
			voidedObjects.remove();
			unvoidedObjects.remove();
		}
	}
}
