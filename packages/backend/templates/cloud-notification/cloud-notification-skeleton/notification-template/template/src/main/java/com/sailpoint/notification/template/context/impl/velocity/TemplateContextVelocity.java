/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.context.impl.velocity;

import com.google.common.collect.Sets;
import com.sailpoint.notification.template.context.TemplateContext;
import org.apache.velocity.VelocityContext;

import java.util.Set;

/**
 * The TemplateContext implementation for Velocity.
 */
public class TemplateContextVelocity implements TemplateContext<String, Object> {

	/**
	 * Velocity relies on VelocityContext where the template model is defined.
	 */
	private final VelocityContext _velocityContext;

	public TemplateContextVelocity() {
		_velocityContext = new VelocityContext();
	}

	public TemplateContextVelocity(TemplateContext templateContext) {
		if (templateContext != null && templateContext.getContext() != null && templateContext.getContext() instanceof VelocityContext) {
			_velocityContext = new VelocityContext((VelocityContext)templateContext.getContext());
		} else {
			_velocityContext = new VelocityContext();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(String key, Object val) {
		_velocityContext.put(key, val);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object get(String key) {
		return _velocityContext.get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> keySet() {
		return Sets.newHashSet(_velocityContext.internalGetKeys());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(String key) {
		return _velocityContext.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getContext() {
		return _velocityContext;
	}
}
