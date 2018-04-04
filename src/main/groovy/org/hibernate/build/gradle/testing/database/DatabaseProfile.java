/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.build.gradle.testing.database;

import java.io.File;
import java.util.Map;

import org.gradle.api.artifacts.Configuration;

/**
 * Contract for database "profiles".  See Readme.md for details.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public interface DatabaseProfile {
	/**
	 * Read access to the name of the profile
	 *
	 * @return The profile name
	 */
	public String getName();

	/**
	 * The base directory for the profile definition
	 *
	 * @return The profile directory for this profile.
	 */
	public File getDirectory();

	/**
	 * Read access to the Hibernate properties contributed by the definition of this profile.
	 *
	 * @return The contributed Hibernate properties
	 */
	public Map<String,Object> getHibernateProperties();

	/**
	 * Read access to the runtime configuration additions contributed by the definition
	 * @return
	 */
	public Configuration getTestingRuntimeConfiguration();
}
