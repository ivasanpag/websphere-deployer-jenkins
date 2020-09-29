package org.jenkinsci.plugins.websphere.services.deployment;

import com.ibm.websphere.management.application.AppConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@ToString
public class Artifact {

    public static final int TYPE_EAR = 1;
    public static final int TYPE_WAR = 2;
    public static final int TYPE_JAR = 3;
    public static final int TYPE_RAR = 4;
    @Getter
    @Setter
    private File sourcePath;
    @Getter
    @Setter
    private String appName;
    @Getter
    @Setter
    private String context;
    @Getter
    @Setter
    private String targets;
    @Getter
    @Setter
    private int type;
    @Getter
    @Setter
    private boolean distribute;
    @Getter
    @Setter
    private boolean precompile;
    @Getter
    @Setter
    private boolean jspReloading;
    @Getter
    @Setter
    private boolean reloading;
    @Getter
    @Setter
    private String installPath;
    @Getter
    @Setter
    private String classLoaderOrder;
    @Getter
    @Setter
    private String classLoaderPolicy;
    @Getter
    @Setter
    private String virtualHost;
    @Getter
    @Setter
    private String edition;
    @Setter
    private List<DeploymentTaskArtifact> deploymentTaskArtifactList;

    private Hashtable<String,Object> preferences;

    public List<DeploymentTaskArtifact> getDeploymentTaskArtifactList() {
        if(deploymentTaskArtifactList == null)
            deploymentTaskArtifactList = new ArrayList<>();
        return deploymentTaskArtifactList;
    }

    public String getTypeName() {
    	switch(type) {
			case TYPE_WAR:
	    		return "war";
	    	case TYPE_JAR:
	    		return "jar";
	    	case TYPE_RAR:
	    		return "rar";
			case TYPE_EAR:
	    	default:
	    		return "ear";

    	}
    }

	public Hashtable<String, Object> getPreferences() {
		if(preferences == null) {
			return new Hashtable<>();
		}
		return new Hashtable<>(preferences);
	}

	public void setPreferences(Hashtable<String, Object> preferences) {
		this.preferences = new Hashtable<>(preferences);
	}

	public int getClassLoaderInt() {
        return classLoaderOrder != null && classLoaderOrder.equalsIgnoreCase("PARENT_LAST") ? 1 : 0;
    }
}
