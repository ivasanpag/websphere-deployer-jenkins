package org.jenkinsci.plugins.websphere.services.deployment;

import lombok.Data;

import java.io.File;

/**
 * @author Greg Peters
 */
@Data
public abstract class AbstractDeploymentService implements DeploymentService {

    private File trustStoreLocation;
    private File keyStoreLocation;
    private String trustStorePassword;
    private String keyStorePassword;
    private String username;
    private String password;
    private String host;
    private String port;

}
