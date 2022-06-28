/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.service;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.exception.NotFoundException;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.sender.email.repository.TenantSenderEmailRepository;
import com.sailpoint.notification.sender.email.service.model.EmailReferencedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sailpoint.notification.context.service.GlobalContextService.BRANDING_CONFIGS;
import static com.sailpoint.notification.context.service.GlobalContextService.DEFAULT_EMAIL_FROM_ADDRESS;
import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_FROM_ADDRESS;

/**
 * TenantSenderEmailService
 */
@CommonsLog
@RequiredArgsConstructor(onConstructor_={@Inject})
public class VerifiedFromAddressService {

	private final MailClient _mailClient;

	private final TenantSenderEmailRepository _tenantSenderEmailRepository;

	private final GlobalContextRepository _globalContextRepository;

	/**
	 * Returns list of tenant sender emails for the given tenant. Verification status may be stale.
	 *
	 * @return The list of all Tenant Sender Emails for the tenant
	 */
	public List<TenantSenderEmail> list() {
		return _tenantSenderEmailRepository.findAllByTenant(RequestContext.ensureGet().getOrg());
	}

	/**
	 * Returns list of tenant sender emails for the given tenant. Pending emails are verified with the service provider
	 * for latest status.
	 *
	 * @return The current list of Tenant Sender Emails for the tenant
	 */
	public List<TenantSenderEmail> listCurrent() {
		List<TenantSenderEmail> tenantSenderEmails = list();

		// Filter for pending emails from the tenant's email list
		List<String> pendingEmails = tenantSenderEmails
				.stream()
				.filter(e -> VerificationStatus.PENDING.name().equals(e.getVerificationStatus()))
				.map(TenantSenderEmail::getEmail)
				.collect(Collectors.toList());

		// Update status if there are pending emails
		if (pendingEmails.size() > 0) {
			Map<String, VerificationStatus> verificationStatusMap = _mailClient.getVerificationStatus(pendingEmails);

			tenantSenderEmails.forEach(e -> {
				if (verificationStatusMap.containsKey(e.getEmail()) && VerificationStatus.PENDING != verificationStatusMap.get(e.getEmail())) {
					e.setVerificationStatus(verificationStatusMap.get(e.getEmail()).name());
					_tenantSenderEmailRepository.save(e);
				}
			});
		}

		// Add sailpoint's no-reply email if it doesn't exist. Log warning message otherwise
		Optional<TenantSenderEmail> spNoReplyEmail = tenantSenderEmails.stream()
				.filter(e -> DEFAULT_EMAIL_FROM_ADDRESS.equals(e.getEmail()))
				.findFirst();
		if (spNoReplyEmail.isPresent()) {
			log.warn("found sailpoint no-reply email");
		} else {
			tenantSenderEmails = new ArrayList<>(tenantSenderEmails);
			tenantSenderEmails.add(TenantSenderEmail.builder()
					.email(DEFAULT_EMAIL_FROM_ADDRESS)
					.verificationStatus(VerificationStatus.SUCCESS.name())
					.build());
		}

		return tenantSenderEmails;
	}

	/**
	 * Initiates the mail verification process with the service provider and saves the tenantSenderEmail
	 * in the repository
	 *
	 * @param tenantSenderEmail
	 */
	public void verify(TenantSenderEmail tenantSenderEmail) {
		log.info("Initiating verify for " + tenantSenderEmail.getEmail());
		_mailClient.verifyAddress(tenantSenderEmail.getEmail());
		save(tenantSenderEmail);
	}

	/**
	 * Saves the given tenantSenderEmail in the repository
	 *
	 * @param tenantSenderEmail
	 */
	public void save(TenantSenderEmail tenantSenderEmail) {
		log.info("Saving " + tenantSenderEmail.getEmail() + " with status " + tenantSenderEmail.getVerificationStatus());
		_tenantSenderEmailRepository.save(tenantSenderEmail);
	}

	/**
	 * Deletes a tenantSenderEmail by id
	 *
	 * @param id
	 */
	public void deleteById(String id) {
		Optional<TenantSenderEmail> tenantSenderEmailOptional = _tenantSenderEmailRepository.findByTenantAndId(RequestContext.ensureGet().getOrg(), id);
		if (tenantSenderEmailOptional.isPresent()) {
			safeDelete(tenantSenderEmailOptional.get());
		} else {
			throw new NotFoundException(TenantSenderEmail.class, id);
		}
	}

	/**
	 * Deletes all the tenantSenderEmails for this tenant
	 *
	 */
	public void deleteAll() {
		list().forEach(this::delete);
	}

	/**
	 * Lists the tenantSenderEmails by email across all tenants.
	 *
	 * WARNING: this returns data that the tenant in request context may not own.
	 *
	 * @param email
	 * @return all the tenantSenderEmails for given email
	 */
	public List<TenantSenderEmail> listByEmail(String email) {
		return _tenantSenderEmailRepository.findAllByEmail(email);
	}

	/**
	 * Batch saves the given tenantSenderEmails
	 *
	 * @param tenantSenderEmails
	 */
	public void batchSave(Collection<TenantSenderEmail> tenantSenderEmails) {
		List<String> existingEmails = list().stream()
				.map(VerifiedFromAddressService::getInternetAddressAsString)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (!existingEmails.isEmpty()) {
			tenantSenderEmails.removeIf(t -> existingEmails.contains(getInternetAddressAsString(t)));
		}
		_tenantSenderEmailRepository.batchSave(tenantSenderEmails);
	}


	/**
	 * Performs a safe delete by first checking for reference in GlobalContext.
	 *
	 * @param tenantSenderEmail
	 * @throws EmailReferencedException
	 * @throws NotFoundException
	 */
	private void safeDelete(TenantSenderEmail tenantSenderEmail) throws EmailReferencedException {
		// Verify that the email is not used in branding from email before deletion
		_globalContextRepository.findAllByTenant(tenantSenderEmail.getTenant())
				.stream()
				.filter(ctx -> {
					try {
						Map brandConfigs = ((Map<String, Object>) ctx.getAttributes().getOrDefault(BRANDING_CONFIGS, Collections.emptyMap()));
						Set<String> usedEmails = (Set<String>) brandConfigs
								.entrySet()
								.stream()
								.map(entry -> {
									Map<String, String> configs = (Map<String, String>) ((Map.Entry) entry).getValue();
									if (configs != null && configs.containsKey(EMAIL_FROM_ADDRESS)) {
										return configs.get(EMAIL_FROM_ADDRESS).toLowerCase();
									}
									return null;
								})
								.filter(Objects::nonNull)
								.collect(Collectors.toSet());
						return usedEmails.contains(tenantSenderEmail.getEmail().toLowerCase());
					} catch (Exception ignored) {
						return false;
					}
				})
				.findAny()
				.ifPresent(ctx -> {
					throw new EmailReferencedException("Email referenced in context " + ctx.getId());
				});

		delete(tenantSenderEmail);
	}

	/**
	 * Deletes the tenantSenderEmail from repo. Also deletes from SES if email is not referenced by any other tenant.
	 *
	 * @param tenantSenderEmail
	 */
	private void delete(TenantSenderEmail tenantSenderEmail) {
		log.info("Deleting " + tenantSenderEmail.getId() + " from the repository");
		_tenantSenderEmailRepository.delete(tenantSenderEmail.getTenant(), tenantSenderEmail.getId());

		// Delete the email from SES only if there is no existing record for this email from other tenant
		if (!DEFAULT_EMAIL_FROM_ADDRESS.equals(tenantSenderEmail.getEmail()) && listByEmail(tenantSenderEmail.getEmail()).size() == 0) {
			log.info("Deleting " + tenantSenderEmail.getEmail() + " from the whitelist");
			_mailClient.deleteAddress(tenantSenderEmail.getEmail());
		}
	}

	/**
	 * Returns the email address from a tenantSenderEmail entity
	 *
	 * @param tenantSenderEmail
	 */
	private static String getInternetAddressAsString(TenantSenderEmail tenantSenderEmail) {
		try {
			InternetAddress internetAddress = new InternetAddress(tenantSenderEmail.getEmail());
			internetAddress.setAddress(tenantSenderEmail.getEmail());
			internetAddress.setPersonal(tenantSenderEmail.getDisplayName());
			return internetAddress.toString();
		} catch (AddressException | UnsupportedEncodingException e) {
			return null;
		}
	}
}
