/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.type.Type;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Analogous to {@link org.hibernate.dialect.function.StandardSQLFunction}
 * except that standard JDBC escape sequences (i.e. {fn blah}) are used when
 * rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class StandardJDBCEscapeFunction extends StandardSQLFunction {
	public StandardJDBCEscapeFunction(String name) {
		super( name );
	}

	public StandardJDBCEscapeFunction(String name, Type typeValue) {
		super( name, typeValue );
	}

	public String render(List args, SessionFactoryImplementor factory) {
		return "{fn " + super.render( args, factory ) + "}";
	}

	public String toString() {
		return "{fn " + getName() + "...}";
	}
}
