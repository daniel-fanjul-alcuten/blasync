package com.spotify.blasync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.spotify.blasync.Client;
import com.spotify.blasync.Message;
import com.spotify.blasync.RequestContext;
import com.spotify.blasync.RequestContextDefaultWrapper;
import com.spotify.blasync.RequestsTemplate;
import com.spotify.blasync.StringDefaultWrapper;
import com.spotify.blasync.User;
import com.spotify.blasync.UserInfo;
import com.spotify.blasync.Wrapper;

public class RequestsTemplateTest {

	@Test
	public void testRequest1_200() {

		// template
		RequestContextDefaultWrapper requestContextWrapper = new RequestContextDefaultWrapper();
		StringDefaultWrapper usernameWrapper = new StringDefaultWrapper();
		Wrapper<User> wrapper = new RequestsTemplate().request1(
				requestContextWrapper, usernameWrapper);

		// given
		RequestContext requestContext = mock(RequestContext.class);
		String username = "foo";
		Client client = mock(Client.class);
		when(requestContext.getClient()).thenReturn(client);
		Message message = new Message(200, new UserInfo("bar", "baz"));
		when(client.send("hm://users/foo")).thenReturn(
				Futures.immediateFuture(message));

		// when
		HashMap<Wrapper<?>, Object> context = new HashMap<Wrapper<?>, Object>();
		context.put(requestContextWrapper, requestContext);
		context.put(usernameWrapper, username);
		User user = wrapper.evaluate(context);

		// then
		assertNotNull(user);
		assertEquals("bar", user.getUsername());
		assertEquals("baz", user.getMetadata());
	}

	@Test
	public void testRequest1_404() {

		// template
		RequestContextDefaultWrapper requestContextWrapper = new RequestContextDefaultWrapper();
		StringDefaultWrapper usernameWrapper = new StringDefaultWrapper();
		Wrapper<User> wrapper = new RequestsTemplate().request1(
				requestContextWrapper, usernameWrapper);

		// given
		RequestContext requestContext = mock(RequestContext.class);
		String username = "foo";
		Client client = mock(Client.class);
		when(requestContext.getClient()).thenReturn(client);
		Message message = new Message(404, new UserInfo("bar", "baz"));
		when(client.send("hm://users/foo")).thenReturn(
				Futures.immediateFuture(message));

		// when
		HashMap<Wrapper<?>, Object> context = new HashMap<Wrapper<?>, Object>();
		context.put(requestContextWrapper, requestContext);
		context.put(usernameWrapper, username);
		User user = wrapper.evaluate(context);

		// then
		assertNull(user);
	}
}
