/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Plugin used to apply notion of database profiles, which are consumed by the matrix testing plugin.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class DatabaseProfilePlugin implements Plugin<Project> {
	public static final String EXTENSION_NAME = "databaseProfiles";

	/**
	 * The directory containing standard database profiles.
	 */
    public static final String STANDARD_DATABASES_DIRECTORY = "databases";
	/**
	 * Names a system setting key that can be set to point to a directory containing additional, custom
	 * database profiles.
	 */
	public static final String CUSTOM_DATABASES_DIRECTORY_KEY = "hibernate-matrix-databases";
	public static final String HIBERNATE_MATRIX_IGNORE = "hibernate-matrix-ignore";

	public static final String MATRIX_BUILD_FILE = "matrix.gradle";
	public static final String JDBC_DIR = "jdbc";

	public void apply(Project project) {
        final DatabaseProfilesExtension extension = project.getExtensions().create(
				EXTENSION_NAME,
				DatabaseProfilesExtension.class,
				project
		);

        // todo : can this always safely be delayed until afterEvaluate?
        project.afterEvaluate(
        		// after the project has been evaluated, come back and resolve
				// the project's database profiles based on the parent project
				// and any locally defined ones
				projectAgain -> extension.resolve()
		);

		project.subprojects(
				subproject -> subproject.getPluginManager().apply( DatabaseProfilePlugin.class )
		);
    }
}
