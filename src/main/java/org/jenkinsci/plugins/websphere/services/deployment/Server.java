package org.jenkinsci.plugins.websphere.services.deployment;

import lombok.Data;

@Data
public class Server implements Comparable<Server>{

    private String objectName;
    private String target;
    private boolean selected;
    private int index;

    @Override
    public int compareTo(Server other) {
        return getTarget().compareTo(other.getTarget());
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Server) {
            Server otherServer = (Server)other;
            if(otherServer.getTarget().equals(getTarget())) {
                if(otherServer.getObjectName().equals(getObjectName())) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public int hashCode() {
        int hashcode = 13;
        if(getTarget() != null) {
            hashcode *= getTarget().hashCode();
        }
        if(getObjectName() != null) {
            hashcode *= getObjectName().hashCode();
        }
        return hashcode;
    }

}
