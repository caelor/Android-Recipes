package uk.me.plek.kitchenRecipes;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class BasicAuthenticator extends Authenticator {

	private final String _username;
	private final String _password;

	public BasicAuthenticator(String username, String password) {
		super();
		
		this._username = username;
		this._password = password;
	}
	
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(this._username, this._password.toCharArray());
	}
}
