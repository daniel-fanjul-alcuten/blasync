package com.spotify.blasync;

public class Message {

	private final int code;
	private final Object object;

	public Message(int code, Object object) {
		super();
		this.code = code;
		this.object = object;
	}

	public int getCode() {
		return code;
	}

	public Object getObject() {
		return object;
	}
}
