package org.sciserver.springapp.racm.utils.controller;

/*
 * I've started to move bits that deal with authentication to login.UserProfileSource
 * in order to make that part mockable (which is impossible as long as we share functionality
 * through inheritance like this.
 * 
 *  A more idiomatic approach would be to implement these classes using AOP.
 */
public abstract class RACMController {
	public final JsonAPIHelper jsonAPIHelper = new JsonAPIHelper();
}
