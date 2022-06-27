/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.utilities.StringUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S3 Implementation of the IdentityStateRepository
 */
@CommonsLog
public class S3Client {

	private final String _bucketName;
	private final TransferManager _transferManager;

	public S3Client(String bucketName) {
		if (StringUtil.isNullOrEmpty(bucketName)) {
			throw new IllegalArgumentException("IdentityState S3 bucket name is required");
		}

		_bucketName = bucketName;
		_transferManager = TransferManagerBuilder.standard()
			.build();
	}

	public S3Client(String bucketName, TransferManager transferManager) {
		if (StringUtil.isNullOrEmpty(bucketName)) {
			throw new IllegalArgumentException("IdentityState S3 bucket name is required");
		}

		_bucketName = bucketName;
		_transferManager = transferManager;
	}

	public byte[] findByURL(String url) {
		final byte[] s3Bytes;
		try {
			s3Bytes = s3Read(url);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		if (s3Bytes == null) {
			return null;
		}

		return s3Bytes;
	}

	public void saveCompressedBytes(byte[] compressedBytes, String url, OffsetDateTime expiration) {
		final InputStream inputStream = new ByteArrayInputStream(compressedBytes);

		final ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(compressedBytes.length);
		meta.setContentEncoding("gzip");
		meta.setContentType("application/json");
		if (expiration != null) {
			meta.setUserMetadata(Collections.singletonMap("expiration", expiration.toString()));
		}

		final Upload upload = _transferManager.upload(_bucketName, url, inputStream, meta);

		try {
			upload.waitForCompletion();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}

	}

	public void deleteByURL(String url) {
		final AmazonS3 s3 = _transferManager.getAmazonS3Client();
		final DeleteObjectRequest req = new DeleteObjectRequest(_bucketName, url);
		s3.deleteObject(req);
	}

	public void deleteAllByTenant(TenantId tenantId) {
		final AmazonS3 s3 = _transferManager.getAmazonS3Client();

		ListObjectsV2Result listResult = null;

		while (listResult == null || listResult.isTruncated()) {
			ListObjectsV2Request listRequest = new ListObjectsV2Request()
				.withBucketName(_bucketName)
				.withPrefix(tenantId.getId());

			if (listResult != null) {
				listRequest.withContinuationToken(listResult.getNextContinuationToken());
			}

			listResult = s3.listObjectsV2(listRequest);

			final List<DeleteObjectsRequest.KeyVersion> keys = listResult.getObjectSummaries()
				.stream()
				.map(S3ObjectSummary::getKey)
				.map(DeleteObjectsRequest.KeyVersion::new)
				.collect(Collectors.toList());

			if (!keys.isEmpty()) {
				final DeleteObjectsRequest req = new DeleteObjectsRequest(_bucketName);
				req.setKeys(keys);


				final int deletedCount = size(s3.deleteObjects(req).getDeletedObjects());
				log.info("deleted " + deletedCount + " objects from s3");
			}
		}
	}

	private static int size(Collection<?> c) {
		if (c == null) {
			return 0;
		}

		return c.size();
	}

	private byte[] s3Read(String key) throws IOException {
		final AmazonS3 s3 = _transferManager.getAmazonS3Client();


		final S3Object object;

		try {
			object = s3.getObject(_bucketName, key);
		} catch (AmazonS3Exception ex) {
			if (ex.getStatusCode() == 404) {
				return null;
			}

			throw ex;
		}

		if (object == null) {
			return null;
		}

		try (InputStream inputStream = object.getObjectContent()) {
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			IOUtils.copy(inputStream, bytes);

			return bytes.toByteArray();
		}
	}
}
