package com.spotify.blasync;

import com.google.common.util.concurrent.ListenableFuture;

public interface Client {

	ListenableFuture<Message> send(String uri);
}
