package jetbrains.buildserver.sonarplugin.sqrunner;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildserver.sonarplugin.Constants;
import jetbrains.buildserver.sonarplugin.Util;
import jetbrains.buildserver.sonarplugin.sqrunner.manager.SQSInfo;
import jetbrains.buildserver.sonarplugin.sqrunner.manager.SQSManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildserver.sonarplugin.sqrunner.manager.SQSManager.ProjectAccessor.recurse;

/**
 * Created by Andrey Titov on 6/2/14.
 *
 * SonarQube Server parameters provider. Resolves SQS parameters by it's ID before build is started.
 */
public class SQSPropertiesProvider implements BuildStartContextProcessor {
    @NotNull
    private final ProjectManager myProjectManager;
    @NotNull
    private final SQSManager mySqsManager;

    public SQSPropertiesProvider(final @NotNull ProjectManager projectManager, final @NotNull SQSManager sqsManager) {
        myProjectManager = projectManager;
        mySqsManager = sqsManager;
    }

    public void updateParameters(final @NotNull BuildStartContext context) {
        for (SRunnerContext runnerContext : context.getRunnerContexts()) {
            if (Constants.RUNNER_TYPE.equals(runnerContext.getType())) {
                final String serverId = runnerContext.getParameters().get(Constants.SONAR_SERVER_ID);
                if (serverId != null) {
                    final SProject project = myProjectManager.findProjectById(context.getBuild().getProjectId());
                    if (project != null) {
                        final SQSInfo server = mySqsManager.findServer(recurse(project), serverId);
                        if (server != null) {
                            addIfNotNull(runnerContext, Constants.SONAR_HOST_URL, server.getUrl());
                            addIfNotNull(runnerContext, Constants.SONAR_LOGIN, server.getLogin());
                            addIfNotNull(runnerContext, Constants.SONAR_SERVER_JDBC_URL, server.getJDBCUrl());
                            addIfNotNull(runnerContext, Constants.SONAR_SERVER_JDBC_USERNAME, server.getJDBCUsername());
                            if (!Util.isEmpty(context.getSharedParameters().get(Constants.SECURE_TEAMCITY_PASSWORD_PREFIX + Constants.SONAR_PASSWORD))) {
                                runnerContext.addRunnerParameter(Constants.SONAR_PASSWORD, "%" + Constants.SECURE_TEAMCITY_PASSWORD_PREFIX + Constants.SONAR_PASSWORD + "%");
                            }
                            if (!Util.isEmpty(context.getSharedParameters().get(Constants.SECURE_TEAMCITY_PASSWORD_PREFIX + Constants.SONAR_SERVER_JDBC_PASSWORD))) {
                                runnerContext.addRunnerParameter(Constants.SONAR_SERVER_JDBC_PASSWORD, "%" + Constants.SECURE_TEAMCITY_PASSWORD_PREFIX + Constants.SONAR_SERVER_JDBC_PASSWORD + "%");
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void addIfNotNull(final @NotNull SRunnerContext runnerContext,
                                     final @NotNull String key,
                                     final @Nullable String value) {
        if (!Util.isEmpty(value)) {
            runnerContext.addRunnerParameter(key, value);
        }
    }
}
