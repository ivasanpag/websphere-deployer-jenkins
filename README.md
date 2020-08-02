Websphere Deployer Plugin (For Jenkins) v2
=========================

This plugin is a fork of Jenkins plugin - [websphere-deployer-plugin](https://github.com/jenkinsci/websphere-deployer-plugin) - to deploy applications in Websphere 8.5.5 and Websphere 9.0.X fully configurable. It uses all deploying code from Jenkins plugin.

This code has been optimized for applications that needs to be installed in "detailed mode" during the installation. It allows to configure EJB-JNDI bindings, sharedLibraries and so on.


![](jenkins.png)

Dependencies
=========================
Plugin depends on some IBM libraries. They must be installed in your local repository to compile the plugin and copied in the Jenkins lib to build the project:

> **com.ibm.ws.admin.client_8.5.0.jar** - ($WAS_INSTALL_ROOT/runtimes)
> **com.ibm.ws.orb_8.5.0.jar** - ($WAS_INSTALL_ROOT/runtimes)

## Configuration

WebSphere Application Server deployment


|       Property     |         Function              |     Example                    |
|----------------|-------------------------------|-----------------------------|
|	WebSphere IP/DNS            |  `  Application server host name`          | `192.168.56.1`              |
|Port            |  `  Application server SOAP port`          | `8880`              |
|Connector Type	            |  `Connector type`          | `SOAP`              |
|Username            |  `  Application server host name`          | `admin`              |
|Password            |  `  Application server host name`          | `mypassword`              |
|Connect Using Global Security            |  ` "Global Security" is enabled in the Administration Console for IBM WebSphere Application Server  `          | `true`              |
|ClientKeyFile            |  `  Absolute path to copy of Client Key file`          | `/var/jenkins_home/certs/DummyClientKeyFile.jks`              |
|ClientKeyPassword            |  ` Key file password`          | `WebAS`              |
|ClientTrustFile            |  `  Absolute path to copy of Trust Key file`          | `/var/jenkins_home/certs/DummyClientTrustFile.jks`              |
|ClientTrustPassword            |  `   Trust file password`          | `WebAS`              |
|Operations            |  ` Specify the operations you want this job to do `          | `Install/Reinstall Application(s)`              |
|Rollback On Error            |  ` Specify if the module will be rolled back if it fails to deploy successfully `          | `true`              |
|Application Name            |  ` Name of the application that will show up in the administration console `          | `MyApp`              |
|EAR/WAR Path            |  ` Specify the EAR or War files you want deployed `          | `modules/**/lastSuccessful/**/myapp.ear`              |
|Application Edition            |  `  If an edition name is established, the application will not start automatically after installing it. `          | `false`              |
|Deployment Targets            |  `  Deployment targets`          | `WebSphere:cell=Cell01,cluster=MyCluster`              |
|Deployment Timeout (minutes)            |  `  Maximum time in minutes to wait before stopping the deployment`          | `35`              |
|Generated EAR Level            |  ` Wrap the WAR artifact in an EAR and deploy it to WebSphere`          | `JavaEE 7`              |
|Generated Context	            |  `  Path for the application`          | `/app`              |
|Virtual Host            |  `  Virtual host to use for this application`          | `default_host`              |
|Remote App. Install Path            |  `  Remote path where the application will be installed `          | ``              |
|Deploy if build is unstable	            |  `  Check if you want to deploy when the build is unstable`          | `false`              |
|Deployment Task Options            |  `  Advance deployment task options`          | `Review the section 'Deployment Task Option'`              |
|Precompile JSPs            |  ` Specify whether to precompile JavaServer Pages (JSP) files as a part of installation`          | `false`              |
|Reloading	            |  `  Path for the application`          | `/app`              |
|JSP Reloading	            |  `  Reload JSP pages`          | `false`              |
|Verbose Output            |  ` Verbose logs`          | `true`              |
|Distribute Application	            |  `  Application binaries for installed applications are expanded to the directory specified`          | `true`              |
|Issue Full Synchronization            |  `  A full synchronization on all nodes before the application is started.`          | `35`              |
|Application Class Loader Order           |  `Specifies whether the class loader searches in the parent class loader or in the application class loader first to load a class`          | `Default`              |
|Application Class Loader Policy	            |  `Load new classes for an application in the order specified in this setting.`          | `Default`              |

 		

## Deployment Task Options

Deployment Task Options has been included to modify all the necessary options during the installation. You can specify one or more deployment options using the following form. Eg: The task "MapSharedLibForMod" installs shared libraries in your application.

 ```javascript
How Works? => TaskName;ColumnToModify;UniqueValueInObject=NewValueToModifyInColumn

Example:
BindJndiForEJBMessageBinding;JNDI;EventPushlet=eis/ProcEventsAS;EventMail=eis/MailEventsAS;Command=eis/CommandsAS;EventSyslog=eis/SyslogEventsAS;QueueProcessor=eis/EventsAS
MapSharedLibForMod;sharedLibName;mywar.war,WEB-INF/web.xml=LIB1+LIB2
MapResRefToEJB;JNDI;javax.sql.DataSource=Datasource_XE

In this way, the plugin will search the value "EventPushlet" in each row in the task "BindJndiForEJBMessageBinding". If it is found, the plugin will modify the column JNDI with the value "eis/ProcEventAS" 


```
> You can find more information about the deployment tasks and its columns  [here](https://www.ibm.com/support/knowledgecenter/SSEQTP_9.0.5/com.ibm.websphere.base.doc/ae/rxml_taskoptions.html)

