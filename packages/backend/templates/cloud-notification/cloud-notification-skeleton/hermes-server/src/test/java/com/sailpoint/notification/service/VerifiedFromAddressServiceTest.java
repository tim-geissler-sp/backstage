/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.exception.NotFoundException;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.sender.email.repository.TenantSenderEmailRepository;
import com.sailpoint.notification.sender.email.service.model.EmailReferencedException;
import org.apache.commons.collections4.map.SingletonMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.sailpoint.notification.context.service.GlobalContextService.BRANDING_CONFIGS;
import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_FROM_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VerifiedFromAddressServiceTest
 */
@RunWith(MockitoJUnitRunner.class)
public class VerifiedFromAddressServiceTest {

    public static final TenantSenderEmail tenantSenderEmail1 = TenantSenderEmail.builder()
            .tenant("acme-solar")
            .displayName("Magnus Carlsen")
            .email("drnykterstein@test.org")
            .id(UUID.randomUUID().toString())
            .verificationStatus(VerificationStatus.PENDING.toString())
            .build();
    @Mock
    MailClient _mailClient;

    @Mock
    TenantSenderEmailRepository _tenantSenderEmailRepository;

    @Mock
    GlobalContextRepository _globalContextRepository;

    VerifiedFromAddressService _verifiedFromAddressService;

    @Before
    public void setup() {
        _verifiedFromAddressService = new VerifiedFromAddressService(_mailClient, _tenantSenderEmailRepository, _globalContextRepository);
    }

    @Test
    public void testListCurrent() {
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1);

        TenantSenderEmail output = _verifiedFromAddressService.listCurrent().get(0);
        assertEquals(tenantSenderEmail1, output);

        // Simulate verification has taken place
        when(_mailClient.getVerificationStatus(Collections.singletonList(tenantSenderEmail1.getEmail())))
                .thenReturn(Collections.singletonMap(tenantSenderEmail1.getEmail(), VerificationStatus.SUCCESS));

        output = _verifiedFromAddressService.listCurrent().get(0);
        assertEquals(tenantSenderEmail1.toBuilder().verificationStatus(VerificationStatus.SUCCESS.name()).build(), output);
    }

    @Test
    public void testVerify() {
        _verifiedFromAddressService.verify(tenantSenderEmail1);

        verify(_mailClient, times(1)).verifyAddress(tenantSenderEmail1.getEmail());
        verify(_tenantSenderEmailRepository, times(1)).save(tenantSenderEmail1);
    }

    @Test
    public void testSave() {
        _verifiedFromAddressService.save(tenantSenderEmail1);
        verify(_tenantSenderEmailRepository, times(1)).save(tenantSenderEmail1);
    }

    @Test(expected = EmailReferencedException.class)
    public void testSafeDeleteEmailReferencedInDefaultBrand() {
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1);

        Map defaultBrand = ImmutableMap.of(EMAIL_FROM_ADDRESS, tenantSenderEmail1.getEmail());
        GlobalContext context = new GlobalContext("acme-solar");
        context.setAttributes(new SingletonMap(BRANDING_CONFIGS, ImmutableMap.of("default", defaultBrand)));
        when(_globalContextRepository.findAllByTenant("acme-solar")).thenReturn(Collections.singletonList(context));

        _verifiedFromAddressService.deleteById(tenantSenderEmail1.getId());
    }

    @Test(expected = EmailReferencedException.class)
    public void testSafeDeleteEmailReferencedInOtherBrand() {
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1);

        Map brand1 = ImmutableMap.of(EMAIL_FROM_ADDRESS, tenantSenderEmail1.getEmail().toUpperCase());
        GlobalContext context = new GlobalContext("acme-solar");
        context.setAttributes(new SingletonMap(BRANDING_CONFIGS, ImmutableMap.of("brand1", brand1)));
        when(_globalContextRepository.findAllByTenant("acme-solar")).thenReturn(Collections.singletonList(context));

        _verifiedFromAddressService.deleteById(tenantSenderEmail1.getId());
    }

    @Test(expected = NotFoundException.class)
    public void testSafeDeleteNonExistant() {
        givenRequestContext();

        when(_tenantSenderEmailRepository.findByTenantAndId(any(), any())).thenReturn(Optional.empty());

        when(_globalContextRepository.findAllByTenant("acme-solar")).thenReturn(Collections.singletonList(new GlobalContext("acme-solar")));

        _verifiedFromAddressService.deleteById("blah@meh.com");
    }

    @Test
    public void testSafeDelete(){
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1);

        when(_globalContextRepository.findAllByTenant("acme-solar")).thenReturn(Collections.singletonList(new GlobalContext("acme-solar")));

        _verifiedFromAddressService.deleteById(tenantSenderEmail1.getId());

        verify(_tenantSenderEmailRepository, times(1)).delete(anyString(), anyString());
        verify(_mailClient, times(1)).deleteAddress(anyString());
    }

    @Test
    public void testSafeDeleteEmailUsedElseWhere(){
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1);

        when(_tenantSenderEmailRepository.findAllByEmail(any())).thenReturn(Collections.singletonList(TenantSenderEmail.builder().build()));
        when(_globalContextRepository.findAllByTenant("acme-solar")).thenReturn(Collections.singletonList(new GlobalContext("acme-solar")));

        _verifiedFromAddressService.deleteById(tenantSenderEmail1.getId());

        verify(_tenantSenderEmailRepository, times(1)).delete(anyString(), anyString());
        verify(_mailClient, never()).deleteAddress(anyString());
    }

    @Test
    public void deleteAll() {
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1, tenantSenderEmail1.toBuilder().email("another@mail.com").build());

        _verifiedFromAddressService.deleteAll();

        verify(_tenantSenderEmailRepository, times(1)).findAllByTenant(eq("acme-solar"));
        verify(_tenantSenderEmailRepository, times(2)).delete(eq("acme-solar"), any());
    }

    @Test
    public void testBatchSaveDuplication() {
        givenRequestContext();
        givenRepo("acme-solar", tenantSenderEmail1);

        TenantSenderEmail tenantSenderEmail2 = tenantSenderEmail1.toBuilder().displayName("Magzy").build();

        _verifiedFromAddressService.batchSave(Sets.newHashSet(tenantSenderEmail2, tenantSenderEmail1));

        ArgumentCaptor<Collection> tenantSenderEmailsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(_tenantSenderEmailRepository, times(1)).batchSave(tenantSenderEmailsCaptor.capture());

        assertEquals(1, tenantSenderEmailsCaptor.getValue().size());
    }

    private void givenRequestContext() {
        OrgData orgData = new OrgData();
        orgData.setPod("dev");
        orgData.setOrg("acme-solar");

        RequestContext requestContext = new RequestContext();
        requestContext.setSecurityContext(new AdministratorSecurityContext());
        requestContext.setOrgData(orgData);
        RequestContext.set(requestContext);
    }

    private void givenRepo(String tenant, TenantSenderEmail ... tenantSenderEmails) {
        when(_tenantSenderEmailRepository.findAllByTenant(tenant)).thenReturn(Arrays.asList(tenantSenderEmails));
        when(_tenantSenderEmailRepository.findByTenantAndId(tenant, tenantSenderEmails[0].getId()))
                .thenReturn(Optional.of(tenantSenderEmails[0]));
    }
}
