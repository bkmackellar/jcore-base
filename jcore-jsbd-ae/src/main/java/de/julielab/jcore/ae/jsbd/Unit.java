/** 
 * Unit.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 * 
 * Current version: 1.6	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * The Unit object.
 * 
 **/

package de.julielab.jcore.ae.jsbd;


public class Unit {

	public int begin; // start offset
	public int end;  // end offset
	public String rep; // string representation of this unit
	public boolean isTokenInternal;
	public String label;  // the predicted label
	
	
	public Unit(int begin, int end, String rep, boolean isTokenInternal) {
		this.begin=begin;
		this.end=end;
		this.rep=rep;
		this.isTokenInternal = isTokenInternal;
		this.label="O";
	}

	
	public String toString() {
		return rep+": " + begin+"-"+end;
	}
}
