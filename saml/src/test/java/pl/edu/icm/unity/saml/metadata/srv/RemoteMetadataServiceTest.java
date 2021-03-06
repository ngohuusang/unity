/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

public class RemoteMetadataServiceTest
{
	private ExecutorsService executorsService;
	private MetadataDownloader downloader;
	
	@Before
	public void init() throws Exception
	{
		executorsService = mock(ExecutorsService.class);
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		when(executorsService.getService()).thenReturn(pool);
		downloader = mock(MetadataDownloader.class);
		when(downloader.getFresh("url", null)).thenAnswer((a) -> {
			String xml = IOUtils.toString(new FileInputStream("src/test/resources/unity-as-sp-meta.xml"));
			return EntitiesDescriptorDocument.Factory.parse(xml);
		});
	}
	
	@Test
	public void shouldCreateHandlerForFirstConsumer()
	{
		RemoteMetadataService service = new RemoteMetadataService(executorsService, downloader);
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		service.registerConsumer("url", 100, null, m -> gotEvent.set(true));
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
	}

	@Test
	public void shouldCreateHandlerFor2ndConsumerOtherURL()
	{
		RemoteMetadataService service = new RemoteMetadataService(executorsService, downloader);
		
		service.registerConsumer("url1", 100, null, m -> {});
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		service.registerConsumer("url2", 100, null, m -> gotEvent.set(true));
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
	}

	@Test
	public void shouldReuseHandlerFor2ndConsumerSameURL() throws Exception
	{
		RemoteMetadataService service = new RemoteMetadataService(executorsService,
				downloader);
		when(downloader.getCached("url")).thenAnswer((a) -> {
			String xml = IOUtils.toString(new FileInputStream("src/test/resources/unity-as-sp-meta.xml"));
			return Optional.of(EntitiesDescriptorDocument.Factory.parse(xml));
		});
		
		service.registerConsumer("url", 200, null, m -> {});
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		service.registerConsumer("url", 200, null, m -> gotEvent.set(true));
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
		verify(downloader, atMost(1)).getFresh(ArgumentMatchers.anyString(), ArgumentMatchers.any());
	}

	@Test
	public void unregistredConsumerIsRemovedFromHandler() throws InterruptedException
	{
		RemoteMetadataService service = new RemoteMetadataService(executorsService, downloader);
		
		AtomicInteger gotEvent = new AtomicInteger(0);
		String id = service.registerConsumer("url", 25, null, m -> gotEvent.incrementAndGet());
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get()>0);
		service.unregisterConsumer(id);
		int events = gotEvent.get();
		Thread.sleep(100);
		assertThat(events, is(gotEvent.get()));
	}
}
