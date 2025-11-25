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
        this(classLoader, new CachingMetadataReaderFactory(classLoader), new PathMatchingResourcePatternResolver(classLoader));
    }

    public EventClassScanner(ClassLoader classLoader, MetadataReaderFactory metadataReaderFactory, ResourcePatternResolver resourceResolver) {
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
