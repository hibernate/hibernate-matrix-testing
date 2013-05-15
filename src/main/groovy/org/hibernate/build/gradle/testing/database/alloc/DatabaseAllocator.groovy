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
package org.hibernate.build.gradle.testing.database.alloc

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.hibernate.build.gradle.testing.database.DatabaseProfile

/**
 * Delegate for managing dynamic database instance allocation as part of the testing lifecycle.
 *
 *
 *
 * Helper for dealing with the "DB Allocator" service set up in the JBoss/Red Hat QE lab.
 *
 * Use the <code>hibernate-matrix-dballocation</code> setting to control db allocation.  By default,
 * no allocations are performed.  <code>hibernate-matrix-dballocation</code> could be either:<ul>
 *     <li><b>all</b> - allocate all non-ignored databases</li>
 *     <li><b>profile1{,profile2,...}</b> - allocate only the named profiles, provided the name is also one of the supported names</li>
 * </ul>
 *
 * @author mvecera
 * @author Strong Liu
 * @author Steve Ebersole
 */
class DatabaseAllocator {
    private static final Logger log = Logging.getLogger( DatabaseAllocator.class );

    public static final String DB_ALLOCATOR_KEY = "dbAllocator";

    private final Project rootProject;

    private final DatabaseAllocationCleanUp cleanUpListener;
    private List<DatabaseAllocationProvider> providers;
    private Map<String,DatabaseAllocation> databaseAllocationMap = new HashMap<String, DatabaseAllocation>();


    /**
     * Get the allocator delegate for this project
     *
     * @param project The project
     *
     * @return The allocator delegate
     */
    public static DatabaseAllocator locate(Project project) {
        if ( ! project.rootProject.hasProperty( DB_ALLOCATOR_KEY ) ) {
            project.rootProject.ext.setProperty( DB_ALLOCATOR_KEY, new DatabaseAllocator( project.rootProject ) );
        }
        return (DatabaseAllocator) project.rootProject.properties[ DB_ALLOCATOR_KEY ];
    }

    private DatabaseAllocator(Project rootProject) {
        this.rootProject = rootProject

        cleanUpListener = new DatabaseAllocationCleanUp();
        project.getGradle().addBuildListener( listener );
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    public void registerProvider(DatabaseAllocationProvider provider) {
        if ( provider == null ) {
            return;
        }

        log.lifecycle( "Registering DatabaseAllocationProvider : ${provider}" )

        if ( providers == null ) {
            providers = new ArrayList<>();
        }
        providers += provider;
    }

    public DatabaseAllocation getAllocation(DatabaseProfile profile) {
		DatabaseAllocation databaseAllocation = databaseAllocationMap.get( profile.name );
		if ( databaseAllocation == null ) {
			databaseAllocation = createAllocation( profile );
			databaseAllocationMap.put( profile.name, databaseAllocation );
		}
		return databaseAllocation;
	}

	private DatabaseAllocation createAllocation(DatabaseProfile profile) {
        for ( DatabaseAllocationProvider provider : providers ) {
            final DatabaseAllocation allocation = provider.buildAllocation( rootProject, profile, cleanUpListener )
            if ( allocation != null ) {
                return allocation;
            }
        }
        return new NoAllocation();
	}
}
