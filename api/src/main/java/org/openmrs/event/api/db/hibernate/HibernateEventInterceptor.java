package org.openmrs.event.api.db.hibernate;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event;
import org.openmrs.event.Event.Action;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes in a single hibernate session and fires the necessary events. Any changes/inserts/deletes
 * made to the DB that are not made through the application won't be detected by the module. We use
 * a Stack here to handle any nested transactions that may occur within a single thread
 */
@Component
public class HibernateEventInterceptor extends EmptyInterceptor {
	
	private static final long serialVersionUID = 1L;
	
	protected final Log log = LogFactory.getLog(HibernateEventInterceptor.class);
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> inserts = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> updates = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> deletes = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> retiredObjects = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> unretiredObjects = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> voidedObjects = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	private ThreadLocal<Stack<HashSet<OpenmrsObject>>> unvoidedObjects = new ThreadLocal<Stack<HashSet<OpenmrsObject>>>();
	
	/**
	 * @see EmptyInterceptor#afterTransactionBegin(Transaction)
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
		
		initializeStackIfNecessary();
		
		inserts.get().push(new HashSet<OpenmrsObject>());
		updates.get().push(new HashSet<OpenmrsObject>());
		deletes.get().push(new HashSet<OpenmrsObject>());
		retiredObjects.get().push(new HashSet<OpenmrsObject>());
		unretiredObjects.get().push(new HashSet<OpenmrsObject>());
		voidedObjects.get().push(new HashSet<OpenmrsObject>());
		unvoidedObjects.get().push(new HashSet<OpenmrsObject>());
	}
	
	/**
	 * @see EmptyInterceptor#onSave(Object, Serializable, Object[], String[], Type[])
	 */
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof OpenmrsObject) {
			inserts.get().peek().add((OpenmrsObject) entity);
		}
		
		//tells hibernate that there are no changes made here that
		//need to be propagated to the persistent object and DB
		return false;
	}
	
	/**
	 * @see EmptyInterceptor#onFlushDirty(Object, Serializable, Object[], Object[], String[],
	 *      Type[])
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
	                            String[] propertyNames, Type[] types) {
		
		if (entity instanceof OpenmrsObject) {
			OpenmrsObject object = (OpenmrsObject) entity;
			updates.get().peek().add(object);
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
									unretiredObjects.get().peek().add(object);
								else
									retiredObjects.get().peek().add(object);
							} else {
								if (previousValue)
									unvoidedObjects.get().peek().add(object);
								else
									voidedObjects.get().peek().add(object);
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
	 * @see EmptyInterceptor#onDelete(Object, Serializable, Object[], String[], Type[])
	 */
	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof OpenmrsObject) {
			deletes.get().peek().add((OpenmrsObject) entity);
		}
	}
	
	/**
	 * @see EmptyInterceptor#onCollectionUpdate(Object, Serializable)
	 */
	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		if (collection != null) {
			//If a collection element has been added/removed, fire an update event for the parent entity
			Object owningObject = getOwner(collection);
			if (owningObject instanceof OpenmrsObject) {
				updates.get().peek().add((OpenmrsObject) owningObject);
			}
		}
	}
	
	/**
	 * Gets the owning object of a persistent collection
	 *
	 * @param collection the persistent collection
	 * @return the owning object
	 */
	private Object getOwner(Object collection) {
		Class<?> pCollClass;
		try {
			pCollClass = Context.loadClass("org.hibernate.collection.PersistentCollection");
		}
		catch (ClassNotFoundException oe) {
			//We are running against openmrs core 2.0 or later where it's a later hibernate version
			try {
				pCollClass = Context.loadClass("org.hibernate.collection.spi.PersistentCollection");
			}
			catch (ClassNotFoundException ie) {
				throw new HibernateException(ie);
			}
		}
		
		Method method = ReflectionUtils.findMethod(pCollClass, "getOwner");
		
		return ReflectionUtils.invokeMethod(method, collection);
	}
	
	/**
	 * @see EmptyInterceptor#afterTransactionCompletion(Transaction)
	 */
	@Override
	public void afterTransactionCompletion(Transaction tx) {
		
		try {
			if (tx.wasCommitted()) {
				for (OpenmrsObject delete : deletes.get().peek()) {
					Event.fireAction(Action.PURGED.name(), delete);
				}
				for (OpenmrsObject insert : inserts.get().peek()) {
					Event.fireAction(Action.CREATED.name(), insert);
				}
				for (OpenmrsObject update : updates.get().peek()) {
					Event.fireAction(Action.UPDATED.name(), update);
				}
				for (OpenmrsObject retired : retiredObjects.get().peek()) {
					Event.fireAction(Action.RETIRED.name(), retired);
				}
				for (OpenmrsObject unretired : unretiredObjects.get().peek()) {
					Event.fireAction(Action.UNRETIRED.name(), unretired);
				}
				for (OpenmrsObject voided : voidedObjects.get().peek()) {
					Event.fireAction(Action.VOIDED.name(), voided);
				}
				for (OpenmrsObject unvoided : unvoidedObjects.get().peek()) {
					Event.fireAction(Action.UNVOIDED.name(), unvoided);
				}
			}
		}
		finally {
			//cleanup
			inserts.get().pop();
			updates.get().pop();
			deletes.get().pop();
			retiredObjects.get().pop();
			unretiredObjects.get().pop();
			voidedObjects.get().pop();
			unvoidedObjects.get().pop();
			
			removeStackIfEmpty();
		}
	}
	
	private void initializeStackIfNecessary() {
		if (inserts.get() == null) {
			inserts.set(new Stack<HashSet<OpenmrsObject>>());
		}
		if (updates.get() == null) {
			updates.set(new Stack<HashSet<OpenmrsObject>>());
		}
		if (deletes.get() == null) {
			deletes.set(new Stack<HashSet<OpenmrsObject>>());
		}
		if (retiredObjects.get() == null) {
			retiredObjects.set(new Stack<HashSet<OpenmrsObject>>());
		}
		if (unretiredObjects.get() == null) {
			unretiredObjects.set(new Stack<HashSet<OpenmrsObject>>());
		}
		if (voidedObjects.get() == null) {
			voidedObjects.set(new Stack<HashSet<OpenmrsObject>>());
		}
		if (unvoidedObjects.get() == null) {
			unvoidedObjects.set(new Stack<HashSet<OpenmrsObject>>());
		}
	}
	
	private void removeStackIfEmpty() {
		if (inserts.get().empty()) {
			inserts.remove();
		}
		if (updates.get().empty()) {
			updates.remove();
		}
		if (deletes.get().empty()) {
			deletes.remove();
		}
		if (retiredObjects.get().empty()) {
			retiredObjects.remove();
		}
		if (unretiredObjects.get().empty()) {
			unretiredObjects.remove();
		}
		if (voidedObjects.get().empty()) {
			voidedObjects.remove();
		}
		if (unvoidedObjects.get().empty()) {
			unvoidedObjects.remove();
		}
	}
}
