package org.jenkinsci.plugins.websphere.utils;

import com.ibm.websphere.management.application.client.AppDeploymentTask;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.websphere.services.deployment.DeploymentTaskArtifact;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class DeploymentTaskUtils {

    public static String prettyAppDeploymentTask(AppDeploymentTask task) {

        StringBuilder prettyTask = new StringBuilder();
        // Columns
        prettyTask.append(task.getName() + "\n");
        for (int i = 0; i < task.getColumnNames().length; i++) {
            String columnName = task.getColumnNames()[i];

            if (i == (task.getColumnNames().length - 1))
                prettyTask.append(columnName);
            else
                prettyTask.append(columnName + ", ");


        }

        // Data
        prettyTask.append("\n");
        String[][] data = task.getTaskData();
        if(data != null) {
            for (int i = 1; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    if(StringUtils.trimToNull(data[i][j]) == null)
                        prettyTask.append("(empty)");
                    else
                        prettyTask.append(data[i][j]);

                    if (j != (data[i].length - 1))
                        prettyTask.append(", ");
                }
                prettyTask.append("\n");

            }
        } else {
            prettyTask.append("No data found");
            prettyTask.append("\n");
        }


        return prettyTask.toString();
    }


    public static List<DeploymentTaskArtifact> fillDeploymentTaskArtifacts(String deploymentTaskProperties) {
        List<DeploymentTaskArtifact> deploymentTaskArtifactList = new ArrayList<>();
        for (StringTokenizer st = new StringTokenizer(deploymentTaskProperties.trim(), "\r\n"); st.hasMoreTokens(); ) {
            deploymentTaskArtifactList.add(new DeploymentTaskArtifact(st.nextToken()));
        }
        return deploymentTaskArtifactList;
    }
}
