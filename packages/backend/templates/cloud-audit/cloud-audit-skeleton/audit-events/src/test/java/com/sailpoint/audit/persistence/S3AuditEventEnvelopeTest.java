package com.sailpoint.audit.persistence;

import com.sailpoint.atlas.search.util.JsonUtils;
import org.junit.Test;
import sailpoint.object.AuditEvent;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Basic POJO sanity checks.
 */
public class S3AuditEventEnvelopeTest {

	@Test
	public void testAuditEventEnvelope() throws NoSuchAlgorithmException {

		S3AuditEventEnvelope env = new S3AuditEventEnvelope();

		String uuid = UUID.randomUUID().toString();
		env.setS3ObjectKey(uuid);
		assertEquals(uuid, env.getS3ObjectKey());

		uuid = UUID.randomUUID().toString();
		env.setTenantId(uuid);
		assertEquals(uuid, env.getTenantId());

		uuid = UUID.randomUUID().toString();
		env.setAuditEventId(uuid);
		assertEquals(uuid, env.getAuditEventId());

		uuid = UUID.randomUUID().toString();
		env.setSha256Hash(uuid);
		assertEquals(uuid, env.getSha256Hash());

		uuid = UUID.randomUUID().toString();
		HashMap<String, String> preJsonMap = new HashMap<>();
		preJsonMap.put("id", uuid);
		String json = JsonUtils.toJson(preJsonMap);
		env.setAuditEventJson(json);
		assertEquals(json, env.getAuditEventJson());

		AuditEvent auditEvent = new AuditEvent();
		auditEvent.setId(UUID.randomUUID().toString());
		env.setAuditEvent(auditEvent);
		assertEquals(auditEvent, env.getAuditEvent());
		assertEquals(auditEvent.getId(), env.getAuditEvent().getId());

		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(json.getBytes(StandardCharsets.UTF_8));
		byte[] digest = md.digest();
		String myHash = DatatypeConverter.printHexBinary(digest).toUpperCase();
		env.setMd5Checksum(myHash);
		assertEquals(myHash, env.getMd5Checksum());

		env.setAlreadyExistedInS3(true);
		assertTrue(env.isAlreadyExistedInS3());

		env.setAlreadyExistedInS3(false);
		assertFalse(env.isAlreadyExistedInS3());

		env.setPreviousDiffered(true);
		assertTrue(env.isPreviousDiffered());

		env.setPreviousDiffered(false);
		assertFalse(env.isPreviousDiffered());

	}

}
