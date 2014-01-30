package com.spotify.blasync;

import java.io.Serializable;
import java.util.List;

public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String username;

	private String metadata;

	private List<PlaylistInfo> playlists;

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

	public List<PlaylistInfo> getPlaylists() {
		return playlists;
	}

	public void setPlaylists(List<PlaylistInfo> playlists) {
		this.playlists = playlists;
	}
}
