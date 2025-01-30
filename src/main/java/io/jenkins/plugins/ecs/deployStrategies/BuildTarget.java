package io.jenkins.plugins.ecs.deployStrategies;

public class BuildTarget {
    private final String taskDefinition;
    private final String repository;
    private final String imageName;
    private final String clusterArn;
    private final String service;
    private final String buildNumber;

    public BuildTarget(String taskDefinition, String repository, String imageName, String clusterArn, String service, String buildNumber) {
        this.taskDefinition = taskDefinition;
        this.repository = repository;
        this.imageName = imageName;
        this.clusterArn = clusterArn;
        this.service = service;
        this.buildNumber = buildNumber;
    }

    public String getTaskDefinition() {
        return taskDefinition;
    }

    public String getRepository() {
        return repository;
    }

    public String getImageName() {
        return imageName;
    }

    public String getClusterArn() {
        return clusterArn;
    }

    public String getService() {
        return service;
    }

    public String getBuildNumber() {
        return buildNumber;
    }
}
