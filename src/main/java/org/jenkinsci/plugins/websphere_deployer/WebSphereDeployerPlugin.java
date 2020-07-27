package org.jenkinsci.plugins.websphere_deployer;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import lombok.Data;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.websphere.services.deployment.Artifact;
import org.jenkinsci.plugins.websphere.services.deployment.DeploymentServiceException;
import org.jenkinsci.plugins.websphere.services.deployment.Server;
import org.jenkinsci.plugins.websphere.services.deployment.WebSphereDeploymentService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.ibm.icu.text.SimpleDateFormat;

/**
 * A Jenkins plugin for deploying to WebSphere either locally or remotely.
 *
 * @author Greg Peters
 */
@Data
public class WebSphereDeployerPlugin extends Notifier {

	private final static String OPERATION_REINSTALL = "1";
    private final String ipAddress;
    private final String connectorType;
    private final String port;
    private final String artifacts;
    private final String earLevel;
    private final String deploymentTimeout;
    private final String classLoaderOrder;
    private final String classLoaderPolicy;
    private final String operations;
    private final String context;
    private final String installPath;
    private final String targets;
    private final String applicationName;
    private final String virtualHost;
    private final String sharedLibName;
    private final String edition;
    private final String datasource;
    private final boolean fullSynchronization;
    private final boolean precompile;
    private final boolean reloading;
    private final boolean jspReloading;
    private final boolean verbose;
    private final boolean distribute;
    private final boolean rollback;
    private final boolean unstableDeploy;
    private final boolean forceUninstallation;
    private final WebSphereSecurity security;

    @DataBoundConstructor
    public WebSphereDeployerPlugin(String ipAddress,
                                   String connectorType,
                                   String port,
                                   String installPath,
                                   WebSphereSecurity security,
                                   String artifacts,
                                   String earLevel,
                                   String deploymentTimeout,
                                   String operations,
                                   String context,
                                   String targets,
                                   String applicationName,
                                   String virtualHost,
                                   String sharedLibName,
                                   String edition,
                                   boolean fullSynchronization,
                                   boolean precompile,
                                   boolean reloading,
                                   boolean jspReloading,
                                   boolean verbose,
                                   boolean distribute,
                                   boolean rollback,
                                   boolean unstableDeploy,
                                   boolean forceUninstallation,
                                   String classLoaderPolicy,
                                   String classLoaderOrder,
                                   String datasource) {
    	this.context = context;
    	this.targets = targets;
    	this.installPath = installPath;
        this.ipAddress = ipAddress;        
        this.connectorType = connectorType;
        this.artifacts = artifacts;
        this.port = port;
        this.operations = operations;
        this.earLevel = earLevel;
        this.deploymentTimeout = deploymentTimeout;
        this.edition = edition;
        this.fullSynchronization = fullSynchronization;
        this.precompile = precompile;
        this.reloading = reloading;
        this.jspReloading = jspReloading;
        this.verbose = verbose;
        this.distribute = distribute;
        this.rollback = rollback;
        this.unstableDeploy = unstableDeploy;
        this.security = security;
        this.classLoaderPolicy = classLoaderPolicy;
        this.classLoaderOrder = classLoaderOrder;
        this.applicationName = applicationName;
        this.virtualHost = virtualHost;
        this.sharedLibName = sharedLibName;
        this.forceUninstallation = forceUninstallation;
        this.datasource = datasource;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	if(build == null) {
    		throw new IllegalStateException("Build cannot be null");
    	}
    	Result buildResult = build.getResult();
    	if(buildResult == null) {
    		throw new IllegalStateException("Build result cannot be null");
    	}
        if(shouldDeploy(buildResult)) {
        	WebSphereDeploymentService service = new WebSphereDeploymentService();
        	Artifact artifact = null;
            try {            	
                EnvVars env = build.getEnvironment(listener);
                preInitializeService(listener,service, env);  
            	service.connect();                	               
                for(FilePath path: gatherArtifactPaths(build, listener)) {
                    artifact = createArtifact(path,listener,service);   
                    log(listener,"Artifact " + artifact.getAppName() + " is being deployed to virtual host: "+artifact.getVirtualHost());
                    stopArtifact(artifact,listener,service);
                    if(getOperations().equals(OPERATION_REINSTALL)) {
                    	uninstallArtifact(artifact,listener,service);
                    	deployArtifact(artifact,listener,service);
                    } else { //otherwise update application
                    	if(!service.isArtifactInstalled(artifact)) {
                    		deployArtifact(artifact, listener, service); //do initial deployment
                    	} else {
                    		updateArtifact(artifact,listener,service);
                    	}
                    }
                    if(isFullSynchronization()) {
                    	service.fullyResynchronizeNodes();
                    }

                    log(listener,"Searching all resources for " + artifact.getSharedLibName() );
                    searchAll(artifact, listener, service);

                    if(StringUtils.trimToNull(artifact.getSharedLibName()) != null) {
                        String[] sharedLibraries = artifact.getSharedLibName().split(",");
                        log(listener,"SharedLibraries to use " + artifact.getSharedLibName() );



                    }
                    startArtifact(artifact,listener,service);

                    if(rollback) {
                    	saveArtifactToRollbackRepository(build, listener, artifact);
                    }
                }
            } catch (Exception e) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream p = null;
				try {
					p = new PrintStream(out,true,"UTF-8");
				} catch (UnsupportedEncodingException e2) {
					e2.printStackTrace();
				}
                e.printStackTrace(p);
                if(verbose) {
                	try {
						logVerbose(listener,"Error deploying to IBM WebSphere Application Server: "+new String(out.toByteArray(),"UTF-8"));
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
                } else {
                	log(listener,"Error deploying to IBM WebSphere Application Server: "+e.getMessage());
                }
                rollbackArtifact(service,build,listener,artifact);
                build.setResult(Result.FAILURE);
            } finally {
                service.disconnect();
            }
        } else {
            listener.getLogger().println("Unable to deploy to IBM WebSphere Application Server, Build Result = " + buildResult);
        }
        return true;
    }

    private void searchAll(Artifact artifact, BuildListener listener, WebSphereDeploymentService service) {
        if(service.isArtifactInstalled(artifact)) {
            listener.getLogger().println("SearchAll Application '"+artifact.getAppName()+"'...");
            service.searchAll(artifact);
        }
    }

    private boolean shouldDeploy(Result result) {
        if (result.equals(Result.SUCCESS)) return true;
        if (unstableDeploy && result.equals(Result.UNSTABLE)) return true;
        return false;
    }
    
    private void log(BuildListener listener,String data) {
		listener.getLogger().println(data);
    }
    
    private void logVerbose(BuildListener listener,String data) {
    	if(verbose) {
    		log(listener,data);
    	}
    }
    
    private void rollbackArtifact(WebSphereDeploymentService service,AbstractBuild build,BuildListener listener,Artifact artifact) {
    	if(build == null) {
    		log(listener,"Cannot rollback to previous verions: build is null");
    	}
    	if(artifact == null) {
    		log(listener,"Cannot rollback to previous version: artifact is null");
    		return;
    	}
    	FilePath workspace = build.getWorkspace();
    	if(workspace == null) {
    		log(listener,"Cannot rollback to previous version: workspace is null");
    		return;
    	}
    	String remote = workspace.getRemote();
    	if(remote == null) {
    		log(listener,"Cannot rollback to previous version: remote path is null");
    		return;
    	}
    	log(listener,"Performing rollback of '"+artifact.getAppName()+"'");    	
    	File installablePath = new File(remote+File.separator+"Rollbacks"+File.separator+artifact.getAppName()+"."+artifact.getTypeName());    	
    	if(installablePath.exists()) {
    		artifact.setSourcePath(installablePath);
    		try {
    			updateArtifact(artifact, listener, service);
    			startArtifact(artifact,listener,service);
    			log(listener,"Rollback of '"+artifact.getAppName()+"' was successful");
    		} catch(Exception e) {
    			e.printStackTrace();
    			log(listener, "Error while trying to rollback to previous version: "+e.getMessage());
    		}
    	} else {
    		log(listener,"WARNING: Artifact doesn't exist rollback repository");
    	}
    }
    
    private void saveArtifactToRollbackRepository(AbstractBuild build,BuildListener listener,Artifact artifact) {
    	listener.getLogger().println("Performing save operations on '" + artifact.getAppName() + "' for future rollbacks");
    	FilePath workspace = build.getWorkspace();
    	if(workspace == null) {
    		log(listener, "Failed to save rollback to repository: Build workspace is null");
    		throw new IllegalStateException("Failed to save rollback to repository: Build workspace is null");
    	}
    	String remote = workspace.getRemote();
    	if(remote == null) {
    		log(listener,"Failed to save rollback to repository: Build workspace remote path is null");
    		throw new IllegalStateException("Failed to save rollback to repository: Build workspace remote path is null");
    	}
    	File rollbackDir = new File(remote+File.separator+"Rollbacks");
    	createIfNotExists(listener, rollbackDir);
    	logVerbose(listener, "Rollback Path: "+rollbackDir.getAbsolutePath());
    	File destination = new File(rollbackDir,artifact.getAppName()+"."+artifact.getTypeName());
    	if(destination.exists()) {
    		log(listener, "Deleting old rollback version...");	
    		if(!destination.delete()) {
    			log(listener, "Failed to delete old rollback version, permissions?: "+destination.getAbsolutePath());
    			return;
    		}
    	}
    	log(listener, "Saving new rollback version...");
    	if(!artifact.getSourcePath().renameTo(destination)) { 
    		logVerbose(listener, "Failed to save '"+artifact.getAppName() +"' to rollback repository");
    	} else {
    		log(listener,"Saved '"+artifact.getAppName()+"' to rollback repository");
    	}
    }
    
    private void createIfNotExists(BuildListener listener,File directory) {
    	if(directory.exists() || directory.mkdir()) {
    		return;
    	}
    	throw new DeploymentServiceException("Failed to create directory, is write access allowed?: "+directory.getAbsolutePath());
    }

    private void deployArtifact(Artifact artifact,BuildListener listener,WebSphereDeploymentService service) throws Exception {
        listener.getLogger().println("Deploying '" + artifact.getAppName() + "' to IBM WebSphere Application Server");
        service.installArtifact(artifact);
    }

    private void uninstallArtifact(Artifact artifact,BuildListener listener,WebSphereDeploymentService service) throws Exception {
        if(service.isArtifactInstalled(artifact)) {
            listener.getLogger().println("Uninstalling Old Application '"+artifact.getAppName()+"'...");
            service.uninstallArtifact(artifact);
        }
    }

    private void startArtifact(Artifact artifact,BuildListener listener,WebSphereDeploymentService service) throws Exception {
		if(StringUtils.trimToNull(artifact.getEdition()) != null) {
			listener.getLogger().println(artifact.getAppName()+ " will not be started automatically because 'Edition' management was used in the jenkins configuration");
			return;
		}
    	listener.getLogger().println("Starting Application '"+artifact.getAppName()+"'...");
    	try {
    		service.startArtifact(artifact, Integer.parseInt(getDeploymentTimeout()));
    	} catch(NumberFormatException e) {
    		service.startArtifact(artifact);
    	}
    }

    private void stopArtifact(Artifact artifact,BuildListener listener,WebSphereDeploymentService service) throws Exception {
        if(service.isArtifactInstalled(artifact)) {
            listener.getLogger().println("Stopping Existing Application '"+artifact.getAppName()+"'...");
            service.stopArtifact(artifact);
        }
    }
    
    private void updateArtifact(Artifact artifact,BuildListener listener,WebSphereDeploymentService service) throws Exception {
        if(service.isArtifactInstalled(artifact)) {
            listener.getLogger().println("Updating '" + artifact.getAppName() + "' on IBM WebSphere Application Server");
            service.updateArtifact(artifact);
        }
    }      

    private Artifact createArtifact(FilePath path,BuildListener listener,WebSphereDeploymentService service) {
        Artifact artifact = new Artifact();
        if(path.getRemote().endsWith(".ear")) {
            artifact.setType(Artifact.TYPE_EAR);
        } else if(path.getRemote().endsWith(".war")) {
            artifact.setType(Artifact.TYPE_WAR);
        }
        if(StringUtils.trimToNull(context) != null) {
        	artifact.setContext(context);
        }                
        if(StringUtils.trimToNull(virtualHost) == null) {
        	artifact.setVirtualHost("default_host");
        } else {
        	artifact.setVirtualHost(virtualHost);
        }
        artifact.setClassLoaderOrder(classLoaderOrder);
        artifact.setClassLoaderPolicy(classLoaderPolicy);
        artifact.setTargets(targets);
        artifact.setInstallPath(installPath);
        artifact.setJspReloading(reloading);
        artifact.setDistribute(distribute);
        if(StringUtils.trimToNull(edition) != null) {
        	artifact.setEdition(edition);	
        }
        if(StringUtils.trimToNull(datasource) != null) {
            artifact.setDatasource(datasource);
        }
        artifact.setPrecompile(isPrecompile());
        artifact.setSourcePath(new File(path.getRemote()));
        if(StringUtils.trimToNull(applicationName) != null) {
        	artifact.setAppName(applicationName);
        } else {
        	artifact.setAppName(getAppName(artifact,service));
        }
        if(artifact.getType() == Artifact.TYPE_WAR) {
            generateEAR(artifact, listener, service);
        }
        return artifact;
    }

    private FilePath[] gatherArtifactPaths(AbstractBuild build,BuildListener listener) throws Exception {
    	if(build == null) {
    		log(listener,"Cannot gather artifact paths: Build is null");
    		throw new IllegalStateException("Cannot gather artifact paths: Build is null");
    	}
    	FilePath workspace = build.getWorkspace();
    	if(workspace == null) {
    		log(listener,"Cannot gather artifact paths: Build workspace is null");
    		throw new IllegalStateException("Cannot gather artifact paths: Build workspace is null");
    	}
    	FilePath workspaceParent = workspace.getParent();
    	if(workspaceParent == null) {
    		log(listener,"Cannot gather artifact paths: Build workspace's parent folder is null");
    		throw new IllegalStateException("Cannot gather artifact paths: Build workspace's parent folder is null");
    	}
    	String artifacts = getArtifacts();
    	if(artifacts == null) {
    		log(listener,"Cannot gather artifact paths: Artifacts are null");
    		throw new IllegalStateException("Cannot gather artifact paths: Artifacts are null");
    	}
        FilePath[] paths = workspaceParent.list(artifacts);
        List<FilePath> pathsList = Stream.of(paths).collect(Collectors.toList());

        if(paths.length == 0) {
            listener.getLogger().println("No deployable artifacts found in path: "+workspaceParent+File.separator+artifacts);
            throw new Exception("No deployable artifacts found!");
        } else {
            listener.getLogger().println("The following artifacts will be deployed in this order...");
            listener.getLogger().println("-------------------------------------------");
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
            List<FilePath> rollbackArtifacts = new ArrayList<>();
            for(FilePath path: pathsList) {
                // Avoid to deploy the artifacts inside Rollback folder
                if(path.getRemote().contains(File.separator + "Rollbacks" + File.separator)) {
                    rollbackArtifacts.add(path);
                } else
                    listener.getLogger().println(path.getRemote()+" Last modified on "+sdf.format(path.lastModified()));
            }
            pathsList.removeAll(rollbackArtifacts);
            listener.getLogger().println("-------------------------------------------");
            listener.getLogger().println("The following artifacts will not be deployed!!");

            for(FilePath rollbackPath: rollbackArtifacts)
                listener.getLogger().println(rollbackPath.getRemote());


            listener.getLogger().println("-------------------------------------------");

        }
        return pathsList.stream().toArray(FilePath[]::new);
    }

    private void preInitializeService(BuildListener listener,WebSphereDeploymentService service,EnvVars env) throws Exception {
        listener.getLogger().println("Connecting to IBM WebSphere Application Server...");
        service.setVerbose(isVerbose());
        service.setBuildListener(listener);;
        service.setConnectorType(getConnectorType());
        service.setHost(env.expand(getIpAddress()));
        service.setPort(env.expand(getPort()));
        if(security != null) {
        	service.setUsername(env.expand(security.getUsername()));
        	service.setPassword(env.expand(security.getPassword()));
        	service.setKeyStoreLocation(new File(env.expand(security.getClientKeyFile())));
        	service.setKeyStorePassword(env.expand(security.getClientKeyPassword()));
        	service.setTrustStoreLocation(new File(env.expand(security.getClientTrustFile())));
        	service.setTrustStorePassword(env.expand(security.getClientTrustPassword()));
        	service.setTrustAll(security.isTrustAll());
        }
    }

    private String getAppName(Artifact artifact,WebSphereDeploymentService service) {
        if(artifact.getType() == Artifact.TYPE_EAR) {
            return service.getAppName(artifact.getSourcePath().getAbsolutePath());
        } else {
            String filename = artifact.getSourcePath().getName();
            return filename.substring(0,filename.lastIndexOf("."));
        }
    }

    private void generateEAR(Artifact artifact,BuildListener listener,WebSphereDeploymentService service) {
        listener.getLogger().println("Generating EAR For Artifact: "+artifact.getAppName());
        log(listener,"Generating EAR For Artifact: "+artifact.getAppName());
        File modified = new File(artifact.getSourcePath().getParent(),artifact.getAppName()+".ear");
        service.generateEAR(artifact, modified, getEarLevel());
        artifact.setSourcePath(modified);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String adminClientPath;
        private String orbClientPath;

        public DescriptorImpl() {
            load();
        }

        public FormValidation doListBean(@QueryParameter("ipAddress")String ipAddress,
                                                 @QueryParameter("connectorType")String connectorType,
                                                 @QueryParameter("port")String port,
                                                 @QueryParameter("username")String username,
                                                 @QueryParameter("password")String password,
                                                 @QueryParameter("trustAll")String trustAll) throws IOException, ServletException {
            WebSphereDeploymentService service = new WebSphereDeploymentService();
            try {
                if(!connectToService(service, connectorType,ipAddress, username, password, port, trustAll)) {
                    String destination = "<Jenkins_Root>"+File.separator+"plugins"+File.separator+"websphere-deployer"+File.separator+"WEB-INF"+File.separator+"lib"+File.separator;
                    return FormValidation.warning("Cannot find the required IBM WebSphere Application Server jar files in '"+destination+"'. Please copy them from IBM WebSphere Application Server (see plugin documentation)");

                }
                List<String> beans = service.listMBean();
                StringBuffer buffer = new StringBuffer();
                buffer.append("\r\n\r\n");
                for(String b: beans) {
                    if(buffer.length() > 0) {
                        buffer.append("\r\n");
                    }
                    buffer.append(b);
                }
                if(buffer.toString().trim().equals("")) {
                    return FormValidation.warning("No beans in WebSphere");
                }
                return FormValidation.ok(buffer.toString());
            } catch (Exception e) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream p = new PrintStream(out,true,"UTF-8");
                e.printStackTrace(p);
                return FormValidation.error("Failed to list beans =>" + new String(out.toByteArray(),"UTF-8"));
            } finally {
                service.disconnect();
            }
        }

        public FormValidation doShowApplications(@QueryParameter("ipAddress")String ipAddress,
                                                 @QueryParameter("connectorType")String connectorType,
                                                 @QueryParameter("port")String port,
                                                 @QueryParameter("username")String username,
                                                 @QueryParameter("password")String password,
                                                 @QueryParameter("trustAll")String trustAll) throws IOException, ServletException {
            WebSphereDeploymentService service = new WebSphereDeploymentService();
            try {
                if(!connectToService(service, connectorType,ipAddress, username, password, port, trustAll)) {
                    String destination = "<Jenkins_Root>"+File.separator+"plugins"+File.separator+"websphere-deployer"+File.separator+"WEB-INF"+File.separator+"lib"+File.separator;
                    return FormValidation.warning("Cannot find the required IBM WebSphere Application Server jar files in '"+destination+"'. Please copy them from IBM WebSphere Application Server (see plugin documentation)");

                }
                List<String> apps = service.listInstalledApps();
                StringBuffer buffer = new StringBuffer();
                buffer.append("\r\n\r\n");
                for(String a: apps) {
                    if(buffer.length() > 0) {
                        buffer.append("\r\n");
                    }
                    buffer.append(a);
                }
                if(buffer.toString().trim().equals("")) {
                    return FormValidation.warning("No apps installed in WebSphere");
                }
                return FormValidation.ok(buffer.toString());
            } catch (Exception e) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream p = new PrintStream(out,true,"UTF-8");
                e.printStackTrace(p);
                return FormValidation.error("Failed to list apps =>" + new String(out.toByteArray(),"UTF-8"));
            } finally {
                service.disconnect();
            }
        }

        public FormValidation doLoadTargets(@QueryParameter("ipAddress")String ipAddress,
                @QueryParameter("connectorType")String connectorType,
                @QueryParameter("port")String port,
                @QueryParameter("username")String username,
                @QueryParameter("password")String password,
                @QueryParameter("trustAll")String trustAll) throws IOException, ServletException {
            WebSphereDeploymentService service = new WebSphereDeploymentService();
            try {

                if(!connectToService(service, connectorType,ipAddress, username, password, port, trustAll)) {
                    String destination = "<Jenkins_Root>"+File.separator+"plugins"+File.separator+"websphere-deployer"+File.separator+"WEB-INF"+File.separator+"lib"+File.separator;
                    return FormValidation.warning("Cannot find the required IBM WebSphere Application Server jar files in '"+destination+"'. Please copy them from IBM WebSphere Application Server (see plugin documentation)");

                }

                List<Server> servers = service.listServers();
                StringBuffer buffer = new StringBuffer();
                buffer.append("\r\n\r\n");
                for(Server server:servers) {
                	if(buffer.length() > 0) {
                		buffer.append("\r\n");
                	}
                	buffer.append(server.getTarget());
                }
                if(buffer.toString().trim().equals("")) {
                	return FormValidation.warning("No server targets are configured in WebSphere");
                }

                return FormValidation.ok(buffer.toString());
            } catch (Exception e) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream p = new PrintStream(out,true,"UTF-8");
                e.printStackTrace(p);
                return FormValidation.error("Failed to list targets =>" + new String(out.toByteArray(),"UTF-8"));
            } finally {
                service.disconnect();
            }
        }

        public FormValidation doTestConnection(@QueryParameter("ipAddress")String ipAddress,
                                               @QueryParameter("connectorType")String connectorType,
                                               @QueryParameter("port")String port,
                                               @QueryParameter("username")String username,
                                               @QueryParameter("password")String password,
                                               @QueryParameter("trustAll")String trustAll) throws IOException, ServletException {
            WebSphereDeploymentService service = new WebSphereDeploymentService();
            try {
                if(!connectToService(service, connectorType,ipAddress, username, password, port, trustAll)) {
                    String destination = "<Jenkins_Root>"+File.separator+"plugins"+File.separator+"websphere-deployer"+File.separator+"WEB-INF"+File.separator+"lib"+File.separator;
                    return FormValidation.warning("Cannot find the required IBM WebSphere Application Server jar files in '"+destination+"'. Please copy them from IBM WebSphere Application Server (see plugin documentation)");
                }

                return FormValidation.ok("Connection Successful!");
            } catch (Exception e) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream p = new PrintStream(out,true,"UTF-8");
                e.printStackTrace(p);
                return FormValidation.error("Connection failed from Jenkins to https://"+ipAddress+":"+port+" => " + new String(out.toByteArray(),"UTF-8"));
            } finally {
                service.disconnect();
            }
        }

        public FormValidation doCheckPort(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Select a port");
            if (value.length() > 5)
                return FormValidation.warning("Cannot be greater than 65535");
            return FormValidation.ok();
        }
        
        public FormValidation doCheckApplicationName(@QueryParameter String value) throws IOException, ServletException {
        	if(StringUtils.trimToNull(value) == null) {
        		return FormValidation.warning("This setting is required for rollback support");
        	} else {
        		return FormValidation.ok();
        	}
        }

        public FormValidation doCheckAdminClientPath(@QueryParameter String value)
                throws IOException, ServletException {
            if(!new File(value).exists()) {
                return FormValidation.error("Path '"+value+"' is not found");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckOrbClientPath(@QueryParameter String value)
                throws IOException, ServletException {
            if(!new File(value).exists()) {
                return FormValidation.error("Path '"+value+"' is not found");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
        	return "Deploy To IBM WebSphere Application Server 8.5.5";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            adminClientPath = formData.getString("adminClientPath");
            orbClientPath = formData.getString("orbClientPath");
            save();
            return super.configure(req,formData);
        }

        public String getAdminClientPath() {
            return adminClientPath;
        }

        public String getOrbClientPath() {
            return orbClientPath;
        }

        private boolean connectToService(WebSphereDeploymentService service, String connectorType,
                                         String ipAddress, String username, String password, String port, String trustAll) throws Exception {
            if(!service.isAvailable()) {
                return false;
            }
            service.setConnectorType(connectorType);
            service.setHost(ipAddress);
            service.setUsername(username);
            service.setPassword(password);
            service.setPort(port);
            service.setTrustAll(Boolean.valueOf(trustAll));
            service.connect();
            return true;
        }
    }
}
