package org.openmrs.event.api.db.hibernate;

import org.apache.commons.lang.BooleanUtils;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.event.EntityEvent;
import org.openmrs.event.Event;
import org.openmrs.event.Event.Action;
import org.openmrs.event.TransactionAfterBeginEvent;
import org.openmrs.event.TransactionBeforeCompletionEvent;
import org.openmrs.event.TransactionCommittedEvent;
import org.openmrs.event.TransactionEvent;
import org.openmrs.event.TransactionNotCommittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A hibernate {@link Interceptor} implementation, intercepts any database inserts, updates and
 * deletes in a single hibernate session and fires the necessary events. Any changes/inserts/deletes
 * made to the DB that are not made through the application won't be detected by the module. We use
 * a Stack here to handle any nested transactions that may occur within a single thread
 */
@Component
public class HibernateEventInterceptor extends EmptyInterceptor implements ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(HibernateEventInterceptor.class);

    private static final long serialVersionUID = 6697237884030315867L;

    private ApplicationEventPublisher eventPublisher;
    private final ThreadLocal<Deque<Set<EntityEvent>>> events = new ThreadLocal<>();

    @Override
    public void setApplicationEventPublisher(@Nullable ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * @param event the event to publish
     */
    private void publishEvent(TransactionEvent event) {
        if (eventPublisher != null) {
            log.trace("Publishing event {}", event);
            eventPublisher.publishEvent(event);
        }
        else {
            throw new IllegalStateException("Unable to publish application event, event publisher is null");
        }
    }

    /**
     * @see EmptyInterceptor#afterTransactionBegin(Transaction)
     */
    @Override
    public void afterTransactionBegin(Transaction tx) {
        log.trace("afterTransactionBegin");
        if (events.get() == null) {
            events.set(new ArrayDeque<>());
        }
        events.get().push(new LinkedHashSet<>());
        tx.registerSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                log.trace("beforeTransactionCompletion");
                publishEvent(new TransactionBeforeCompletionEvent(this, events.get().peek()));
            }
            @Override
            public void afterCompletion(int status) {
                log.trace("afterTransactionCompletion");
                try {
                    if (status == Status.STATUS_COMMITTED) {
                        publishEvent(new TransactionCommittedEvent(this, events.get().peek()));
                    }
                    else {
                        publishEvent(new TransactionNotCommittedEvent(this, events.get().peek(), status));
                    }
                }
                finally {
                    Deque<Set<EntityEvent>> eventStack = events.get();
                    eventStack.pop();
                    if (eventStack.isEmpty()) {
                        events.remove();
                    }
                }
            }
        });
        publishEvent(new TransactionAfterBeginEvent(this, events.get().peek()));
    }

    /**
     * This is called when an entity is created, not when it is updated
     */
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        log.trace("onSave: {}", entity);
        handleEntity(entity, Action.CREATED);
        return false;  //tells hibernate that there are no changes made here that need to be propagated to the persistent object and DB
    }

    /**
     * This is called only when an entity is updated, not when it is created
     * The voided property is a special case that we consider generally as representing a delete/undelete operation
     */
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        log.trace("onFlushDirty: {}", entity);
        handleEntity(entity, Action.UPDATED);
        if (entity instanceof Retireable || entity instanceof Voidable) {
            boolean previousValue = false;
            boolean currentValue = false;
            String property = (entity instanceof Retireable) ? "retired" : "voided";
            for (int i = 0; i < propertyNames.length; i++) {
                String propertyName = propertyNames[i];
                if (propertyName.equals(property)) {
                    previousValue = previousState != null && BooleanUtils.isTrue((Boolean) previousState[i]);
                    currentValue = BooleanUtils.isTrue((Boolean) currentState[i]);
                }
            }
            if (currentValue) {
                if (!previousValue) {
                    handleEntity(entity, entity instanceof Retireable ? Action.RETIRED : Action.VOIDED);
                }
            } else {
                if (previousValue) {
                    handleEntity(entity, entity instanceof Retireable ? Action.UNRETIRED : Action.UNVOIDED);
                }
            }
        }
        return false;
    }

    /**
     * This is invoked when the collection associated with an object is removed
     * Consider this to be an update of the object containing the collection
     */
    @Override
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        log.trace("onCollectionRemove");
        if (collection instanceof PersistentCollection) {
            PersistentCollection persistentCollection = (PersistentCollection) collection;
            handleEntity(persistentCollection.getOwner(), Action.UPDATED);
        }
        else {
            log.trace("collection is not a PersistentCollection");
        }
    }

    /**
     * This is invoked when the collection associated with an object is recreated
     * Consider this to be an update of the object containing the collection
     */
    @Override
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        log.trace("onCollectionRecreate");
        if (collection instanceof PersistentCollection) {
            PersistentCollection persistentCollection = (PersistentCollection) collection;
            handleEntity(persistentCollection.getOwner(), Action.UPDATED);
        }
        else {
            log.trace("collection is not a PersistentCollection");
        }
    }

    /**
     * This is invoked when the collection associated with an object is updated
     * Consider this to be an update of the object containing the collection
     */
    @Override
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        log.trace("onCollectionUpdate");
        if (collection instanceof PersistentCollection) {
            PersistentCollection persistentCollection = (PersistentCollection) collection;
            handleEntity(persistentCollection.getOwner(), Action.UPDATED);
        }
        else {
            log.trace("collection is not a PersistentCollection");
        }
    }

    /**
     * This is called when an entity is deleted
     */
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        log.trace("onDelete: {}", entity);
        handleEntity(entity, Action.PURGED);
    }

    /**
     * Called when an entity is operated upon with the given action
     */
    protected void handleEntity(Object entity, Event.Action action) {
        if (entity instanceof OpenmrsObject) {
            OpenmrsObject openmrsObject = (OpenmrsObject) entity;
            EntityEvent event = new EntityEvent(openmrsObject, action);
            events.get().peek().add(event);
            log.trace("{}", event);
        }
        else {
            log.trace("{} is not an openmrsObject", action);
        }
    }
}
