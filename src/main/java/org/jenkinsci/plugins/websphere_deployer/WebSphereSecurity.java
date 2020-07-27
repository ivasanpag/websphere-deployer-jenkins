package org.jenkinsci.plugins.websphere_deployer;

import hudson.util.Scrambler;

import lombok.Data;
import org.kohsuke.stapler.DataBoundConstructor;

@Data
public class WebSphereSecurity {
	
	private String username;
	private String password;
	private String clientKeyFile;
	private String clientKeyPassword;
	private String clientTrustFile;
	private String clientTrustPassword;
	private boolean trustAll;
	
	@DataBoundConstructor
	public WebSphereSecurity(String username, String password,
			String clientKeyFile, String clientKeyPassword,
			String clientTrustFile, String clientTrustPassword,boolean trustAll) {
		this.username = username;
		setPassword(password);
		this.clientKeyFile = clientKeyFile;
		setClientKeyPassword(clientKeyPassword);
		this.clientTrustFile = clientTrustFile;
		setClientTrustPassword(clientTrustPassword);
		this.trustAll = trustAll;
	}

	public String getPassword() {
		return Scrambler.descramble(password);
	}

	public void setPassword(String password) {
		this.password = Scrambler.scramble(password);
	}

	public String getClientKeyPassword() {
		return Scrambler.descramble(clientKeyPassword);
	}

	public void setClientKeyPassword(String clientKeyPassword) {
		this.clientKeyPassword = Scrambler.scramble(clientKeyPassword);
	}

	public String getClientTrustPassword() {
		return Scrambler.descramble(clientTrustPassword);
	}

	public void setClientTrustPassword(String clientTrustPassword) {
		this.clientTrustPassword = Scrambler.scramble(clientTrustPassword);
	}
}
