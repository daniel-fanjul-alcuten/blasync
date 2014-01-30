package com.spotify.blasync;

import java.io.Serializable;

public class Playlist implements Serializable {

	private static final long serialVersionUID = 1L;

	private int plalistId;

	private String name;

	public Playlist(int plalistId, String name) {
		super();
		this.plalistId = plalistId;
		this.name = name;
	}

	public int getPlalistId() {
		return plalistId;
	}

	public void setPlalistId(int plalistId) {
		this.plalistId = plalistId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
