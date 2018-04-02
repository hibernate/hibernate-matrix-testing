/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

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
import java.util.Objects;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * Database profile as defined by a directory named {@code jdbc} containing JDBC drivers.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class JdbcDirectoryProfile extends AbstractDatabaseProfileImpl {
    private final Configuration jdbcDependencies;

    public JdbcDirectoryProfile(File jdbcDirectory, Project project) {
        super( jdbcDirectory.getParentFile(), project );

        assert jdbcDirectory.isDirectory();

        jdbcDependencies = prepareConfiguration( getName() );
        project.getDependencies().add( getName(), project.files( (Object[]) Objects.requireNonNull( jdbcDirectory.listFiles() ) ) );
    }

    @Override
    public Configuration getTestingRuntimeConfiguration() {
        return jdbcDependencies;
    }
}
