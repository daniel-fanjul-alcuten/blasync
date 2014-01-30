package com.spotify.blasync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.blasync.Client;
import com.spotify.blasync.Message;
import com.spotify.blasync.RequestContext;
import com.spotify.blasync.Requests;
import com.spotify.blasync.User;
import com.spotify.blasync.UserInfo;

public class RequestsTest {

	@Test
	public void testRequest1_200() throws InterruptedException,
			ExecutionException {

		// given
		RequestContext requestContext = mock(RequestContext.class);
		String username = "foo";
		Client client = mock(Client.class);
		when(requestContext.getClient()).thenReturn(client);
		Message message = new Message(200, new UserInfo("bar", "baz"));
		when(client.send("hm://users/foo")).thenReturn(
				Futures.immediateFuture(message));

		// when
		ListenableFuture<User> future = new Requests().request1(requestContext,
				username);
		User user = Futures.getUnchecked(future);

		// then
		assertNotNull(user);
		assertEquals("bar", user.getUsername());
		assertEquals("baz", user.getMetadata());
	}

	@Test
	public void testRequest1_404() throws InterruptedException,
			ExecutionException {

		// given
		RequestContext requestContext = mock(RequestContext.class);
		String username = "foo";
		Client client = mock(Client.class);
		when(requestContext.getClient()).thenReturn(client);
		Message message = new Message(404, new UserInfo("bar", "baz"));
		when(client.send("hm://users/foo")).thenReturn(
				Futures.immediateFuture(message));

		// when
		ListenableFuture<User> future = new Requests().request1(requestContext,
				username);
		User user = Futures.getUnchecked(future);

		// then
		assertNull(user);
	}
}
