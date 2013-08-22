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
package org.hibernate.build.gradle.testing.database.alloc;

import org.gradle.api.tasks.testing.Test;

/**
 * Represents a database instances allocated in the JBoss/Red Hat Qe Lab via {@link DatabaseAllocator}
 * 
 * @author mvecera
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public interface DatabaseAllocation {
	public void prepareForExecution(Test testTask);
	
	/**
	 * Allows the allocation to script tasks to be completed before each test class (not method).  For instance,
	 * the JBoss/RH QA Lab allocation forcefully erases the database to ensure a clean slate.
	 * We don't always do a great job of ensuring the test cases clean up after themselves.
	 */
	public void beforeTestClass();

	public void release();
}
