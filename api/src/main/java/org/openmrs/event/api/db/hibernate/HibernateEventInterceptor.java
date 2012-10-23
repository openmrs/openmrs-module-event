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
import org.openmrs.event.Event;
import org.openmrs.event.Event.Action;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes in a single session hibernate session and fires the necessary events. Any
 * changes/inserts/deletes made to the DB that are not made through the application won't be
 * detected by the module.
 */
public class HibernateEventInterceptor extends EmptyInterceptor {
	
	private static final long serialVersionUID = 1L;
	
	protected final Log log = LogFactory.getLog(HibernateEventInterceptor.class);
	
	private ThreadLocal<HashSet<Object>> inserts = new ThreadLocal<HashSet<Object>>();
	
	private ThreadLocal<HashSet<Object>> updates = new ThreadLocal<HashSet<Object>>();
	
	private ThreadLocal<HashSet<Object>> deletes = new ThreadLocal<HashSet<Object>>();
	
	/**
	 * @see org.hibernate.EmptyInterceptor#afterTransactionBegin(org.hibernate.Transaction)
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
		inserts.set(new HashSet<Object>());
		updates.set(new HashSet<Object>());
		deletes.set(new HashSet<Object>());
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onSave(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (OpenmrsObject.class.isAssignableFrom(entity.getClass())) {
			inserts.get().add((OpenmrsObject) entity);
		}
		
		return false;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onFlushDirty(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
	                            String[] propertyNames, Type[] types) {
		
		if (OpenmrsObject.class.isAssignableFrom(entity.getClass())) {
			updates.get().add((OpenmrsObject) entity);
			//TODO Look at the changes in retired/voided properties and trigger 
			//events for retired, unretired, voided, unvoided if necessary
		}
		
		return false;
	}
	
	/**
	 * @see org.hibernate.EmptyInterceptor#onDelete(java.lang.Object, java.io.Serializable,
	 *      java.lang.Object[], java.lang.String[], org.hibernate.type.Type[])
	 */
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (OpenmrsObject.class.isAssignableFrom(entity.getClass())) {
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
			if (OpenmrsObject.class.isAssignableFrom(owningObject.getClass())) {
				//TODO Add removed element to deletes because they are actually deleted
				//from the database but hibernate doesn't call onDelete() for them
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
				for (Object delete : deletes.get()) {
					Event.fireAction(Action.PURGED.name(), (OpenmrsObject) delete);
				}
				for (Object insert : inserts.get()) {
					Event.fireAction(Action.CREATED.name(), (OpenmrsObject) insert);
				}
				for (Object update : updates.get()) {
					Event.fireAction(Action.UPDATED.name(), (OpenmrsObject) update);
				}
			}
		}
		finally {
			//cleanup
			inserts.remove();
			updates.remove();
			deletes.remove();
		}
	}
}
