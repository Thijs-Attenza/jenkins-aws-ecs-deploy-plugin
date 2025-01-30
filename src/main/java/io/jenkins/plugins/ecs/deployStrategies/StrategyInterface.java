package io.jenkins.plugins.ecs.deployStrategies;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.ecs.AmazonECS;
import hudson.AbortException;
import hudson.Util;
import hudson.model.TaskListener;

public interface StrategyInterface {
    void build(TaskListener listener, AmazonECS client, BuildTarget buildTarget) throws AbortException;

    default AWSCredentialsProvider getCredentialsProvider(TaskListener listener, String assumeRoleArn) {
        //If STS is configured, use the Assume Role Provider
        if (Util.fixEmptyAndTrim(assumeRoleArn) != null) {
            listener.getLogger().println("Assuming role: " + assumeRoleArn);
            return new STSAssumeRoleSessionCredentialsProvider.Builder(assumeRoleArn, "ecs-deploy-session").build();
        } else {
            return new DefaultAWSCredentialsProviderChain();
        }
    }
}
