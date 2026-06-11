/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import javax.jms.MapMessage;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory {@link MapMessage} used by {@link LegacyEventBridge} to deliver payloads to legacy
 * {@link EventListener}s.
 * <p>
 * Backed by a JDK {@link Proxy} so we don't have to stub out the whole JMS Message interface.
 * Only the data accessor methods used by legacy event consumers (the {@code uuid}, {@code classname},
 * {@code action} payload triplet plus topic-style {@link EventMessage} contents) are implemented.
 * Header, property, and acknowledge operations specific to a real JMS provider throw
 * {@link UnsupportedOperationException}.
 * <p>
 * Missing-key handling follows practical JMS provider behavior: numeric getters return 0, the
 * boolean getter returns false, and object/string/bytes getters return null. A char getter on a
 * missing key throws NPE (matching the JMS 1.1 spec — char has no null form).
 */
@Deprecated
final class LegacyMapMessage {

	private LegacyMapMessage() {
	}

	static MapMessage of(EventMessage payload) {
		Map<String, Serializable> data = payload != null
			? new LinkedHashMap<>(payload)
			: Collections.emptyMap();
		return (MapMessage) Proxy.newProxyInstance(
			LegacyMapMessage.class.getClassLoader(),
			new Class<?>[] { MapMessage.class },
			new Handler(data));
	}

	private static final class Handler implements InvocationHandler {

		private final Map<String, Serializable> data;

		Handler(Map<String, Serializable> data) {
			this.data = data;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			switch (method.getName()) {
				case "getString":
					return asString(data.get((String) args[0]));
				case "getObject":
				case "getBytes":
					return data.get((String) args[0]);
				case "getBoolean": {
					Object v = data.get((String) args[0]);
					return v instanceof Boolean ? v : Boolean.parseBoolean(asString(v));
				}
				case "getInt":
					return numberOrZero(data.get((String) args[0])).intValue();
				case "getLong":
					return numberOrZero(data.get((String) args[0])).longValue();
				case "getDouble":
					return numberOrZero(data.get((String) args[0])).doubleValue();
				case "getFloat":
					return numberOrZero(data.get((String) args[0])).floatValue();
				case "getShort":
					return numberOrZero(data.get((String) args[0])).shortValue();
				case "getByte":
					return numberOrZero(data.get((String) args[0])).byteValue();
				case "getChar": {
					// JMS 1.1: char has no null form; reading a missing/null item must NPE
					Object v = data.get((String) args[0]);
					if (v == null) {
						throw new NullPointerException("no item named " + args[0]);
					}
					return v instanceof Character ? v : ((String) v).charAt(0);
				}
				case "itemExists":
					return data.containsKey((String) args[0]);
				case "getMapNames":
					return Collections.enumeration(data.keySet());
				case "toString":
					return "LegacyMapMessage" + data;
				case "equals":
					return proxy == (args == null ? null : args[0]);
				case "hashCode":
					return System.identityHashCode(proxy);
				default:
					throw new UnsupportedOperationException(
						"MapMessage." + method.getName()
							+ " is not supported by the legacy Event bridge; "
							+ "migrate to TransactionEventListener for full event metadata");
			}
		}

		private static String asString(Object v) {
			return v == null ? null : v.toString();
		}

		private static Number numberOrZero(Object v) {
			return v instanceof Number ? (Number) v : 0;
		}
	}
}
