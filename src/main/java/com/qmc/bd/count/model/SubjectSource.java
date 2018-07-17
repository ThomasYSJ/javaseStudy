package com.qmc.bd.count.model;

import java.io.Serializable;

public class SubjectSource implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String _id;
	private String source;
	private String name;
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String toString() {
		return "SubjectSource [_id=" + _id + ", source=" + source + ", name="
				+ name + "]";
	}
	
}
