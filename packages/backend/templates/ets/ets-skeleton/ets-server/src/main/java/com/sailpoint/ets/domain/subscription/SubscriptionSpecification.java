/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.subscription;

import com.google.common.base.Enums;
import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.infrastructure.web.EtsFilter;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.UUID;

/**
 * Spring Data Jpa Specification used for filter querying {@link Subscription}.
 */
@AllArgsConstructor
public class SubscriptionSpecification implements Specification<Subscription> {

	private final EtsFilter _filter;

	/**
	 * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
	 * {@link Root} and {@link CriteriaQuery}.
	 *
	 * @param root    must not be {@literal null}.
	 * @param query   must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 * @return a {@link Predicate}, may be {@literal null}.
	 */
	@Override
	public Predicate toPredicate(Root<Subscription> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		if (_filter == null || _filter.getProperty() == null || _filter.getOperation() == null || _filter.getValue() == null) {
			return null;
		}

		Predicate predicate = null;
		if (_filter.getOperation() == FilterCompiler.LogicalOperation.EQ) {
			switch (_filter.getProperty()) {
				case "tenantId":
					predicate = builder.equal(root.get(_filter.getProperty()), new TenantId(_filter.getValue().toString()));
					break;
				case "id":
					UUID subscriptionId = null;
					try {
						subscriptionId = UUID.fromString(_filter.getValue().toString());
					} catch (Exception ignored) {
					}
					predicate = builder.equal(root.get(_filter.getProperty()), subscriptionId);
					break;
				case "triggerId":
					predicate = builder.equal(root.get(_filter.getProperty()), new TriggerId(_filter.getValue().toString()));
					break;
				case "type":
					predicate = builder.equal(root.get(_filter.getProperty()), Enums.getIfPresent(SubscriptionType.class, _filter.getValue().toString()).orNull());
					break;
				case "name":
					predicate = builder.equal(root.get(_filter.getProperty()), _filter.getValue().toString());
					break;
				default:
					return null;
			}
		}
		if(_filter.isNot() && predicate != null) {
			predicate = predicate.not();
		}
		return predicate;
	}
}
