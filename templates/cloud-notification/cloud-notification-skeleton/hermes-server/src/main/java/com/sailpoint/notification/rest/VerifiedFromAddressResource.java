/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.api.common.AtlasBaseV3Resource;
import com.sailpoint.atlas.api.common.filters.FilterBuilder;
import com.sailpoint.atlas.api.common.filters.ListSorter;
import com.sailpoint.atlas.exception.NotFoundException;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.cloud.api.client.model.errors.ApiExceptionBuilder;
import com.sailpoint.cloud.api.client.model.errors.ErrorDetailCode;
import com.sailpoint.notification.sender.common.rest.NotificationFilter;
import com.sailpoint.notification.sender.common.rest.NotificationFilterBuilder;
import com.sailpoint.notification.sender.common.rest.NotificationQueryOptions;
import com.sailpoint.notification.sender.email.EmailStatusDto;
import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.service.VerifiedFromAddressService;
import com.sailpoint.notification.sender.email.service.model.EmailReferencedException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sailpoint.notification.context.service.GlobalContextService.DEFAULT_EMAIL_FROM_ADDRESS;

/**
 * V3 resource for tenant sender email
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VerifiedFromAddressResource extends AtlasBaseV3Resource<NotificationFilter, NotificationQueryOptions> {

	private static final int EMAIL_LIMIT = 10;

	@Inject
	VerifiedFromAddressService _verifiedFromAddressService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getQueryableFields() {
		return Sets.newHashSet("email");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getSortableFields() {
		return Sets.newHashSet("email");
	}

	@Override
	protected NotificationQueryOptions constructQueryOptions(int offset, int limit, NotificationFilter filter, List<ListSorter> sorters) {
		return new NotificationQueryOptions(offset, limit, filter, sorters);
	}

	@Override
	protected FilterBuilder<NotificationFilter> getFilterBuilder() {
		return new NotificationFilterBuilder();
	}

	@GET
	@Path("verified-from-addresses")
	@RequireRight("idn:notification-verified-from-addresses:read")
	public Response getTenantSenderEmail() {
		List<EmailStatusDto> emailStatusDtoList = _verifiedFromAddressService.listCurrent()
				.stream()
				.map(e -> {
					EmailStatusDto.EmailStatusDtoBuilder builder = EmailStatusDto.builder()
							.id(e.getId())
							.verificationStatus(VerificationStatus.valueOf(e.getVerificationStatus()));
					try {
						builder.email(new InternetAddress(e.getEmail(), e.getDisplayName()).toString());
					} catch (UnsupportedEncodingException ex) {
						builder.email(e.getEmail());
					}
					return builder.build();
				})
				.collect(Collectors.toList());

		return okResponse(emailStatusDtoList);
	}

	@POST
	@Path("verified-from-addresses")
	@RequireRight("idn:notification-verified-from-addresses:write")
	public Response createTenantSenderEmail(EmailStatusDto emailStatusDto) {
		TenantSenderEmail tenantSenderEmail = validateEmail(emailStatusDto, false);

		_verifiedFromAddressService.verify(tenantSenderEmail);

		return createdResponse(emailStatusDto
				.toBuilder()
				.id(tenantSenderEmail.getId())
				.verificationStatus(VerificationStatus.PENDING)
				.build());
	}

	@POST
	@Path("assign-verified-from-addresses")
	@RequireRight("idn:notification-assign-verified-from-addresses:write")
	public Response assignTenantSenderEmail(EmailStatusDto emailStatusDto) {
		TenantSenderEmail tenantSenderEmail = validateEmail(emailStatusDto, true);

		_verifiedFromAddressService.save(tenantSenderEmail);

		return createdResponse(emailStatusDto
				.toBuilder()
				.id(tenantSenderEmail.getId())
				.verificationStatus(VerificationStatus.SUCCESS)
				.build());
	}

	@DELETE()
	@Path("verified-from-addresses/{id}")
	@RequireRight("idn:notification-verified-from-addresses:delete")
	public Response deleteTenantSenderEmail(@PathParam("id") String id) {
		try {
			_verifiedFromAddressService.deleteById(id);
			return Response.noContent().build();
		} catch (EmailReferencedException e) {
			throw new ApiExceptionBuilder()
					.detailCode(ErrorDetailCode.INVALID_REQUEST_IN_CURRENT_STATE)
					.build();
		} catch (NotFoundException n) {
			throw new ApiExceptionBuilder()
					.detailCode(ErrorDetailCode.REFERENCED_OBJECT_NOT_FOUND)
					.params("id", id)
					.build();
		}
	}


	/**
	 * Validate the email dto
	 * @param emailStatusDto the DTO from HTTP request
	 * @param assigningVerifiedEmail flag to check email status if assigning email to another tenant
	 * @return verified {@link TenantSenderEmail}
	 */
	private TenantSenderEmail validateEmail(EmailStatusDto emailStatusDto, boolean assigningVerifiedEmail) {

		// Build the email
		TenantSenderEmail tenantSenderEmail = TenantSenderEmail.builder()
				.id(UUID.randomUUID().toString())
				.tenant(RequestContext.ensureGet().getOrg())
				.verificationStatus(VerificationStatus.PENDING.name()).build();

		// Make sure email is valid. Parse email and personal(display name)
		try {
			InternetAddress internetAddress = new InternetAddress(emailStatusDto.getEmail());
			tenantSenderEmail.setEmail(internetAddress.getAddress());
			tenantSenderEmail.setDisplayName(internetAddress.getPersonal());
		} catch (AddressException | NullPointerException e) {
			new ApiExceptionBuilder()
					.detailCode(ErrorDetailCode.ILLEGAL_VALUE)
					.params(emailStatusDto.getEmail(), "email")
					.buildAndThrow();
		}

		// Make sure the email to be created is not sailpoint's no-reply email
		if (DEFAULT_EMAIL_FROM_ADDRESS.equals(tenantSenderEmail.getEmail())) {
			new ApiExceptionBuilder()
					.detailCode(ErrorDetailCode.BAD_REQUEST_CONTENT)
					.messageKey(
							ErrorDetailCode.BAD_REQUEST_CONTENT.getVariantErrorMessageKey("invalid-action"))
					.params(DEFAULT_EMAIL_FROM_ADDRESS)
					.buildAndThrow();
		}

		// Check per tenant email limit
		List<TenantSenderEmail> emailList = _verifiedFromAddressService.list();
		if (emailList.size() >= EMAIL_LIMIT) {
			new ApiExceptionBuilder()
					.detailCode(ErrorDetailCode.LIMIT_VIOLATION)
					.params(EMAIL_LIMIT, "emails")
					.buildAndThrow();
		}

		// If the email exists and the status is FAILED, update id and re-verify the email
		emailList.stream().filter(e -> e.getEmail().equals(tenantSenderEmail.getEmail()))
				.findFirst()
				.ifPresent(e -> {
					if (!VerificationStatus.FAILED.name().equals(e.getVerificationStatus())) {
						new ApiExceptionBuilder()
								.detailCode(ErrorDetailCode.INVALID_REQUEST_IN_CURRENT_STATE)
								.buildAndThrow();
					} else {
						tenantSenderEmail.setId(e.getId());
					}
				});

		// Verify that the email is already in SUCCESS status before assigning to another tenant
		if (assigningVerifiedEmail) {
			_verifiedFromAddressService.listByEmail(tenantSenderEmail.getEmail()).stream()
					.filter(e -> VerificationStatus.SUCCESS.name().equals(e.getVerificationStatus()))
					.findAny()
					.orElseThrow(() -> new ApiExceptionBuilder()
							.detailCode(ErrorDetailCode.INVALID_REQUEST_IN_CURRENT_STATE)
							.build());
			tenantSenderEmail.setVerificationStatus(VerificationStatus.SUCCESS.name());
		}

		return tenantSenderEmail;
	}

	/**
	 * Sort, filter and paginate response based on query parameters
	 * @param list list of EmailStatusDto.
	 * @return Response.
	 */
	private Response okResponse(List<EmailStatusDto> list) {
		NotificationQueryOptions queryOptions = getQueryOptions();

		//sort email list
		if(queryOptions.getSorterList().size() == 1) {
			list.sort((a, b) -> {
				if (queryOptions.getSorterList().get(0).isAscending()) {
					return a.getEmail().compareToIgnoreCase(b.getEmail());
				} else {
					return b.getEmail().compareToIgnoreCase(a.getEmail());
				}
			});
		}

		//filter email list
		list = list.stream()
				.filter(l -> !queryOptions.getFilter().isPresent() ||
						queryOptions.getFilter().get().getValue().toString().equalsIgnoreCase(l.getEmail()))
				.skip(queryOptions.getOffset())
				.limit(queryOptions.getLimit())
				.collect(Collectors.toList());

		// Conditionally add the count header
		if(isCountHeaderRequested()) {
			return okResponse(list, list.size());
		} else {
			return Response.ok(list).build();
		}
	}

}
