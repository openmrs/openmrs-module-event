/**
 * The contents of this file are subject to the OpenMRS Public License Version
 * 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.event.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.openmrs.*;
import org.openmrs.annotation.Handler;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event;
import org.openmrs.event.Event.Action;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is registered against multiple openmrs services to fireevents to
 * the {@link Event} for saving, updating, and deleting.
 */
public class GeneralEventAdvice implements MethodInterceptor {

	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * @see
	 * org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 * @should fire event on create @should fire event on update @should fire
	 * event on updating proxied object @should fire event on creating objects
	 * @should fire event on updating User
	 * @should fire event on creating a global property
	 * @should fire event on editing a global property
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Boolean[] created = null;

		if (isSaveMethod(invocation.getMethod())) {
			Object[] args = invocation.getArguments();
			if (args.length > 0) {
				Collection<?> collection = null;
				if (args[0] instanceof OpenmrsObject) {
					collection = Arrays.asList(args[0]);
				} else if (args[0] instanceof Collection) {
					collection = (Collection<?>) args[0];
				}

				if (collection != null) {
					created = new Boolean[collection.size()];
					int i = 0;
					for (Object object : collection) {
						if (object instanceof OpenmrsObject) {
							try {
								created[i] = (((OpenmrsObject) object).getId() == null);
							} catch (UnsupportedOperationException e) {
								if (object instanceof GlobalProperty) {
									//Check if we have this GP in the DB
									GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyByUuid(
									    ((OpenmrsObject) object).getUuid());
									created[i] = (gp == null) ? true : false;
								} else {
									// do nothing here. just continue silently.
									created[i] = true;
								}
							} catch (Exception e) {
								created[i] = true;
								log.error(
										"Defaults to created since cannot determine based on ID if created or updated for type "
										+ object.getClass().getName(), e);
							}
							i++;
						}
					}
				}
			}
		}

		Object returnValue = invocation.proceed();

		afterReturning(returnValue, invocation.getMethod(), invocation.getArguments(), created, invocation.getThis());

		return returnValue;
	}

	protected void afterReturning(Object returnValue, Method method, Object[] args, Boolean[] created, Object target) {
		//args must be 1 or more than one (e.g. retire has 2)
		if (args.length < 1 || !(args[0] instanceof OpenmrsObject || args[0] instanceof Collection)) {
			return;
		}

		String methodName = method.getName();

		// a cheat to quit early if we don't deal with this type of method
		if (!isSupportedMethodName(methodName)) {
			return;
		}

		// TODO precalculate or cache somewhere so we don't repeat it with every
		// call
		Class serviceClass = method.getDeclaringClass();
		Set<String> supportedClasses = getSupportedClasses(serviceClass);

		// if we know some objects in this service
		if (supportedClasses.size() > 0) {
			Collection<?> collection = null;

			if (args[0] instanceof OpenmrsObject) {
				collection = Arrays.asList(args[0]);
			} else if (args[0] instanceof Collection) {
				collection = (Collection<?>) args[0];
			}

			if (collection != null) {
				int i = 0;
				for (Object object : collection) {
					if (object instanceof OpenmrsObject) {
						OpenmrsObject openmrsObject = (OpenmrsObject) object;

						boolean objectCreated = false;
						if (created != null) {
							objectCreated = created[i++];
						}

						Event.Action action = determineAction(methodName, openmrsObject, objectCreated, supportedClasses);

						if (action != null) {
							Event.fireAction(action.name(), openmrsObject);
						}
					}
				}
			}
		}

	}

	/**
	 * Determines an action based on the given parameters.
	 *
	 * @param object
	 * @param created
	 * @param supportedClasses
	 * @param method
	 * @return Action or null if no
	 */
	protected Action determineAction(String methodName, Object object, boolean created, Set<String> supportedClasses) {

        Class objectClass = Hibernate.getClass(object);
        String objectSimplename = supportedObjectSimplename(objectClass, supportedClasses);

        if (objectSimplename == null) {
            return null;
        }

		if (isActionMethod(methodName, "save", objectSimplename)) {
			if (created) {
				return Event.Action.CREATED;
			} else {
				return Event.Action.UPDATED;
			}
		} else if (isActionMethod(methodName, "retire", objectSimplename)) {
			return Event.Action.RETIRED;
		} else if (isActionMethod(methodName, "unretire", objectSimplename)) {
			return Event.Action.UNRETIRED;
		} else if (isActionMethod(methodName, "void", objectSimplename)) {
			return Event.Action.VOIDED;
		} else if (isActionMethod(methodName, "unvoid", objectSimplename)) {
			return Event.Action.UNVOIDED;
		} else if (isActionMethod(methodName, "purge", objectSimplename)) {
			return Event.Action.PURGED;
		} else {
			return null;
		}
	}

	private boolean isActionMethod(String methodName, String action, String objectSimplename) {
		return methodName.equals(action + objectSimplename)
				|| methodName.equals(action + objectSimplename + "s")
				|| methodName.equals(action + objectSimplename + "es")
				|| methodName.equals(action + objectSimplename.substring(0, objectSimplename.length() - 1) + "ies");
	}

	private boolean isSaveMethod(Method method) {
		return method.getName().startsWith("save");
	}

	/**
	 * @param methodName
	 * @return @should return true for save @should return true for void @should
	 * return true for purge @should return false for update
	 */
	private boolean isSupportedMethodName(String methodName) {
		return methodName.startsWith("save") || methodName.startsWith("retire") || methodName.startsWith("unretire")
				|| methodName.startsWith("void") || methodName.startsWith("unvoid") || methodName.startsWith("purge");
	}

	/**
	 * @param serviceClass
	 * @return @should return Patient for PatientService @should return valid
	 * answer for non core service @should return handler annotated objects
	 */
	public Set<String> getSupportedClasses(Class serviceClass) {
		Set<String> supportedClasses = new HashSet<String>();

		// TODO: add more classes here

		if (PatientService.class.isAssignableFrom(serviceClass)) {
			supportedClasses.add(Patient.class.getName());
			supportedClasses.add(PatientIdentifierType.class.getName());
		} else if (ConceptService.class.isAssignableFrom(serviceClass)) {
			supportedClasses.add(Concept.class.getName());
			supportedClasses.add(ConceptClass.class.getName());
			supportedClasses.add(ConceptDatatype.class.getName());
		} else if (AdministrationService.class.isAssignableFrom(serviceClass)) {
			supportedClasses.add(GlobalProperty.class.getName());
		} else if (UserService.class.isAssignableFrom(serviceClass)) {
			supportedClasses.add(User.class.getName());
		} else if (OrderService.class.isAssignableFrom(serviceClass)) {
			supportedClasses.add(Order.class.getName());
			supportedClasses.add(OrderType.class.getName());
		} else if  (PersonService.class.isAssignableFrom(serviceClass)) {
            supportedClasses.add(Person.class.getName());
        }
        else {
			Handler handler = (Handler) serviceClass.getAnnotation(Handler.class);
			if (handler != null) {
				for (Class<?> cls : handler.supports()) {
					supportedClasses.add(cls.getName());
				}
			}
		}

		return supportedClasses;
	}

    /**
     * If this is supported class, or one of its supertypes is a supported class,
     * this method runs the simple name of that supported class
     *
     * For instance, if TestOrder.class is passed in, this method should return "Order"
     *
     * @param clazz
     * @param supportedClasses
     * @return
     */
    public String supportedObjectSimplename(Class clazz, Set<String> supportedClasses) {

        if (supportedClasses.contains(clazz.getName())) {
            return clazz.getSimpleName();
        }
        if (clazz.getSuperclass() != null) {
            return supportedObjectSimplename(clazz.getSuperclass(), supportedClasses);
        }
        return null;
    }
}
