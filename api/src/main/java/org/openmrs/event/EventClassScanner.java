/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.util.OpenmrsClassLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Utility class that searches the classpath for classes of a given type and its subclasses
 */
public class EventClassScanner implements AutoCloseable {
	
	private static final String PATTERN = "classpath*:org/openmrs/**/*.class";
	
	private final ClassLoader classLoader;
	
	private final MetadataReaderFactory metadataReaderFactory;
	
	private final ResourcePatternResolver resourceResolver;
	
	private volatile Resource[] resources = null;
	
	public EventClassScanner() {
		this(OpenmrsClassLoader.getInstance());
	}
	
	public EventClassScanner(ClassLoader classLoader) {
		this(classLoader, new CachingMetadataReaderFactory(classLoader),
		        new PathMatchingResourcePatternResolver(classLoader));
	}
	
	public EventClassScanner(ClassLoader classLoader, MetadataReaderFactory metadataReaderFactory,
	    ResourcePatternResolver resourceResolver) {
		this.classLoader = classLoader;
		this.metadataReaderFactory = metadataReaderFactory;
		this.resourceResolver = resourceResolver;
	}
	
	@Override
	public void close() {
		if (metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			((CachingMetadataReaderFactory) metadataReaderFactory).clearCache();
		}
	}
	
	/**
	 * Searches for classes extending or implementing the given type.
	 * 
	 * @param type the type to match
	 * @return the list of found classes
	 */
	public <T> List<Class<? extends T>> getClasses(Class<? extends T> type) throws IOException, ClassNotFoundException {
		if (resources == null) {
			synchronized (this) {
				if (resources == null) {
					resources = resourceResolver.getResources(PATTERN);
					if (metadataReaderFactory instanceof CachingMetadataReaderFactory) {
						((CachingMetadataReaderFactory) metadataReaderFactory).setCacheLimit(resources.length);
					}
				}
			}
		}
		
		List<Class<? extends T>> types = new ArrayList<>();
		TypeFilter typeFilter = new AssignableTypeFilter(type);
		
		for (Resource resource : resources) {
			MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
			// we call isConcrete() here first since the metadataReader always initializes the class metadata,
			// so this is should be quicker than checking if the type filter matches
			if (metadataReader.getClassMetadata().isConcrete() && typeFilter.match(metadataReader, metadataReaderFactory)) {
				String classname = metadataReader.getClassMetadata().getClassName();
				types.add((Class<? extends T>) classLoader.loadClass(classname));
			}
		}
		
		return types;
	}
}
