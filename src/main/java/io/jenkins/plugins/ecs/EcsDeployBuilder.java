package io.jenkins.plugins.ecs;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.*;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.ecs.config.PluginConfiguration;
import io.jenkins.plugins.ecs.deployStrategies.BuildNumberStrategy;
import io.jenkins.plugins.ecs.deployStrategies.BuildTarget;
import io.jenkins.plugins.ecs.deployStrategies.LatestStrategy;
import io.jenkins.plugins.ecs.deployStrategies.StrategyInterface;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;
import java.util.Objects;

public class EcsDeployBuilder extends Builder implements SimpleBuildStep {
    private final String clusterArn;
    private final String service;
    private final String taskDefinition;
    private final String imageName;
    private final String assumeRole;
    private final String repository;
    private String deployStrategy;
    public final PluginConfiguration config = PluginConfiguration.getInstance();

    @DataBoundConstructor
    public EcsDeployBuilder(String clusterArn, String service, String taskDefinition, String imageName, String assumeRole, String repository, String deployStrategy) {
        this.clusterArn = clusterArn;
        this.service = service;
        this.taskDefinition = taskDefinition;
        this.imageName = imageName;
        this.assumeRole = assumeRole;
        this.repository = repository;
        this.deployStrategy = deployStrategy;
    }

    public PluginConfiguration getConfig()
    {
        return config;
    }

    public String getClusterArn()
    {
        return clusterArn;
    }

    public String getService() {
        return service;
    }

    public String getTaskDefinition() {
        return taskDefinition;
    }

    public String getImageName() {
        return imageName;
    }

    public String getAssumeRole() {
        return assumeRole;
    }

    public String getDeployStrategy() {
        return deployStrategy;
    }

    @DataBoundSetter
    public void setDeployStrategy(String deployStrategy) {
        this.deployStrategy = deployStrategy;
    }

    public String getRepository() {
        return repository;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws AbortException {
        StrategyInterface strategyInterface = this.getStrategy();
        AWSCredentialsProvider credentialsProvider = strategyInterface.getCredentialsProvider(listener, assumeRole);
        AmazonECS client = AmazonECSClientBuilder.standard().withRegion(config.getClient().getRegion()).withCredentials(credentialsProvider).build();

        strategyInterface.build(listener, client, this.getBuildTarget(env));
    }

    private StrategyInterface getStrategy()
    {
        if (deployStrategy.equals("buildnumber")) {
            return new BuildNumberStrategy();
        }

        return new LatestStrategy();
    }

    private BuildTarget getBuildTarget(EnvVars env)
    {
        return new BuildTarget(
            taskDefinition,
            repository,
            imageName,
            clusterArn,
            service,
            env.get("BUILD_NUMBER")
        );
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.EcsDeployBuilder_DescriptorImpl_DisplayName();
        }

        public ListBoxModel doFillDeployStrategyItems() {
            final ListBoxModel regions = new ListBoxModel();
            regions.add("Latest (Rollback not possible)", "latest");
            regions.add("Build Number (Rollback possible)", "buildnumber");

            return regions;
        }
    }
}

