package com.sailpoint.sp.identity.event.infrastructure.s3;

import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3ClientTest {
	private TransferManager mockTransferManager = mock(TransferManager.class);
	private S3Client s3Client = new S3Client("bucket", mockTransferManager);
	private AmazonS3 mockAmazonS3 = mock(AmazonS3.class);

	@Test
	public void findByURLTest() {
		String s3Data = "data";

		S3Object object = new S3Object();
		InputStream stream = new ByteArrayInputStream(s3Data.getBytes());
		object.setObjectContent(stream);
		when(mockTransferManager.getAmazonS3Client()).thenReturn(mockAmazonS3);
		when(mockAmazonS3.getObject("bucket", "www.s3Url.com")).thenReturn(object);
		byte[] s3Bytes = s3Client.findByURL("www.s3Url.com");

		assertEquals(new String(s3Bytes), s3Data);
		verify(mockAmazonS3, times(1)).getObject("bucket", "www.s3Url.com");

	}

	@Test
	public void deleteByURLTest() {
		String s3Data = "data";

		S3Object object = new S3Object();
		InputStream stream = new ByteArrayInputStream(s3Data.getBytes());
		object.setObjectContent(stream);
		when(mockTransferManager.getAmazonS3Client()).thenReturn(mockAmazonS3);

		s3Client.deleteByURL("www.s3Url.com");
		verify(mockAmazonS3, times(1)).deleteObject(any(DeleteObjectRequest.class));

	}

}
