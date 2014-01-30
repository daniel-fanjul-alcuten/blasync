package com.spotify.blasync;

import java.io.Serializable;

public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String username;

	private String metadata;

	public UserInfo(String username, String metadata) {
		super();
		this.username = username;
		this.metadata = metadata;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}
