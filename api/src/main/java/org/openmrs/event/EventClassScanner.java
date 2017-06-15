/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.api.APIException;
import org.openmrs.util.OpenmrsClassLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Utility class that searches the classpath for classes of a given type and its subclasses
 */
public class EventClassScanner {
	
	private static final String PATTERN = "classpath*:org/openmrs/**/*.class";
	
	private static final EventClassScanner INSTANCE = new EventClassScanner();
	
	private MetadataReaderFactory metadataReaderFactory;
	
	private ResourcePatternResolver resourceResolver;
	
	private EventClassScanner() {
		this.metadataReaderFactory = new SimpleMetadataReaderFactory(OpenmrsClassLoader.getInstance());
		this.resourceResolver = new PathMatchingResourcePatternResolver(OpenmrsClassLoader.getInstance());
	}
	
	public synchronized static EventClassScanner getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Searches for classes extending or implementing the given type.
	 * 
	 * @param type the type to match
	 * @return the list of found classes
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public <T> List<Class<? extends T>> getClasses(Class<? extends T> type) throws IOException, ClassNotFoundException {
		
		List<Class<? extends T>> types = new ArrayList<Class<? extends T>>();
		TypeFilter typeFilter = new AssignableTypeFilter(type);
		Resource[] resources = resourceResolver.getResources(PATTERN);
		OpenmrsClassLoader classLoader = OpenmrsClassLoader.getInstance();
		
		for (Resource resource : resources) {
			MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
			if (typeFilter.match(metadataReader, metadataReaderFactory)) {
				if (metadataReader.getClassMetadata().isConcrete()) {
					String classname = metadataReader.getClassMetadata().getClassName();
					try {
						types.add((Class<? extends T>) classLoader.loadClass(classname));
					}
					catch (ClassNotFoundException e) {
						throw new APIException("Class cannot be loaded: " + classname, e);
					}
				}
			}
		}
		
		return types;
	}
}
