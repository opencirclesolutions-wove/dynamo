/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.ocs.dynamo.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.vaadin.flow.function.SerializablePredicate;

/**
 * A predicate that joins multiple other predicates together
 * 
 * @author Bas Rutten
 *
 * @param <T> the type of the entity to filter
 */
public abstract class CompositePredicate<T> implements SerializablePredicate<T> {

	private static final long serialVersionUID = 8690339909486826760L;

	private final List<SerializablePredicate<T>> operands = new ArrayList<>();

	public List<SerializablePredicate<T>> getOperands() {
		return operands;
	}
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
