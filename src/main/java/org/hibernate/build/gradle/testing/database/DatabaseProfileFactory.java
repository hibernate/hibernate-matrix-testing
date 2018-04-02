/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.build.gradle.testing.database;

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.reflect.ObjectInstantiationException;

/**
 * @author Steve Ebersole
 */
public class DatabaseProfileFactory implements ObjectFactory {
	@Override
	public <T extends Named> T named(Class<T> type, String name) throws ObjectInstantiationException {
		return null;
	}

	@Override
	public <T> T newInstance(Class<? extends T> type, Object... parameters) throws ObjectInstantiationException {
		return null;
	}

	@Override
	public <T> Property<T> property(Class<T> valueType) {
		return null;
	}

	@Override
	public <T> ListProperty<T> listProperty(Class<T> elementType) {
		return null;
	}

	@Override
	public <T> SetProperty<T> setProperty(Class<T> elementType) {
		return null;
	}
}
