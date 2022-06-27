/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.model;

/**
 * Configuration DTO that holds the data required for branding of specific
 * elements in the IDN UI.
 */
public class BrandConfig {

	/**
	 * should be name
	 */
	String _name;

	/**
	 * Customizable product name
	 */
	String _productName;

	/**
	 * The narrow icon URL configured for the brand.
	 */
	String _standardLogoURL;

	/**
	 * The narrow icon URL configured for the brand.
	 */
	String _narrowLogoURL;

	/**
	 * Color of the nav bar.
	 */
	String _navigationColor;

	/**
	 * Color of action buttons
	 */
	String _actionButtonColor;

	/**
	 * Color of activeLinks
	 */
	String _activeLinkColor;

	/**
	 * Brand specific from email address which will be used when sending email.
	 */
	String _emailFromAddress;

	/**
	 * Brand specific login informational message.
	 */
	String _loginInformationalMessage;

	public BrandConfig() {
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getProductName() {
		return _productName;
	}

	public void setProductName(String productName) {
		_productName = productName;
	}

	public String getStandardLogoURL() {
		return _standardLogoURL;
	}

	public void setStandardLogoURL(String standardLogoURL) {
		_standardLogoURL = standardLogoURL;
	}

	public String getNarrowLogoURL() {
		return _narrowLogoURL;
	}

	public void setNarrowLogoURL(String narrowLogoURL) {
		_narrowLogoURL = narrowLogoURL;
	}

	public String getNavigationColor() {
		return _navigationColor;
	}

	public void setNavigationColor(String navigationColor) {
		_navigationColor = navigationColor;
	}

	public String getActionButtonColor() {
		return _actionButtonColor;
	}

	public void setActionButtonColor(String actionButtonColor) {
		_actionButtonColor = actionButtonColor;
	}

	public String getEmailFromAddress() {
		return _emailFromAddress;
	}

	public void setEmailFromAddress(String emailFromAddress) {
		_emailFromAddress = emailFromAddress;
	}

	public String getActiveLinkColor() {
		return _activeLinkColor;
	}

	public void setActiveLinkColor(String activeLinkColor) {
		_activeLinkColor = activeLinkColor;
	}

	public String getLoginInformationalMessage() { return _loginInformationalMessage; }

	public void setLoginInformationalMessage(String loginInformationalMessage) { _loginInformationalMessage = loginInformationalMessage; }

}

