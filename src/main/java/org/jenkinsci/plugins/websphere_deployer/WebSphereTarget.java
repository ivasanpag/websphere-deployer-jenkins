package org.jenkinsci.plugins.websphere_deployer;

import lombok.Data;

@Data
public class WebSphereTarget {

	private String cell;
	private String node;
	private String server;
	private String cluster;
	private boolean selected;

}
