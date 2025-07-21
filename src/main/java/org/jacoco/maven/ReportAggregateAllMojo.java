/*******************************************************************************
 * Copyright (c) 2009, 2025 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Olivier Lamy, John Oliver, Marc R. Hoffmann, Jan Wloka - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.jacoco.report.IReportGroupVisitor;

/**
 * Creates a structured code coverage report (HTML, XML, and CSV) from all
 * projects within the reactor. From those projects class and source files as well as JaCoCo
 * execution data files will be collected and aggregated. This mojo will not
 * fork any lifecycle and so needs to be called after execution data files have been
 * created already.
 * <p>
 * The generated report contains both cross-module and same-module coverage,
 * which means that it replaces regular {@code jacoco:report} goal executions.
 *
 * @since 0.8.13
 */
@Mojo(name = "report-aggregate-all", threadSafe = true, aggregator = true)
public class ReportAggregateAllMojo extends AbstractReportMojo {

    /**
     * A list of execution data files to include in the report from each
     * project. May use wildcard characters (* and ?). When not specified all
     * *.exec files from the target folder will be included.
     */
    @Parameter
    List<String> dataFileIncludes;

    /**
     * A list of execution data files to exclude from the report. May use
     * wildcard characters (* and ?). When not specified nothing will be
     * excluded.
     */
    @Parameter
    List<String> dataFileExcludes;

    /**
     * Output directory for the reports. Note that this parameter is only
     * relevant if the goal is run from the command line or from the default
     * build lifecycle. If the goal is run indirectly as part of a site
     * generation, the output directory configured in the Maven Site Plugin is
     * used instead.
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco-aggregate")
    private File outputDirectory;

    /**
     * Include this project in the report. If true then this projects class and
     * source files as well as JaCoCo execution data files will be collected.
     *
     * @since 0.8.9
     */
    @Parameter(defaultValue = "false")
    private boolean includeCurrentProject;

    /**
     * The projects in the reactor.
     */
    @Parameter(property = "reactorProjects", readonly = true)
    protected List<MavenProject> reactorProjects;

    @Override
    boolean canGenerateReportRegardingDataFiles() {
        return true;
    }

    @Override
    boolean canGenerateReportRegardingClassesDirectory() {
        return true;
    }

    void loadExecutionData(final ReportSupport support) throws IOException {
        // https://issues.apache.org/jira/browse/MNG-5440
        if (dataFileIncludes == null) {
            dataFileIncludes = Arrays.asList("target/*.exec");
        }

        final FileFilter filter = new FileFilter(dataFileIncludes,
                dataFileExcludes);
        loadExecutionData(support, filter, project.getBasedir());
        for (final MavenProject subProject : getSubProjects()) {
            loadExecutionData(support, filter, subProject.getBasedir());
        }
    }

    private void loadExecutionData(final ReportSupport support,
                                   final FileFilter filter, final File basedir) throws IOException {
        for (final File execFile : filter.getFiles(basedir)) {
            support.loadExecutionData(execFile);
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
        getLog().info("Jacoco reports aggregated \uD83E\uDD56\uD83E\uDD56");
    }

    @Override
    File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    void createReport(final IReportGroupVisitor visitor,
                      final ReportSupport support) throws IOException {
        final IReportGroupVisitor group = visitor.visitGroup(title);
        if (includeCurrentProject) {
            processProject(support, group, project);
        }
        for (final MavenProject subProject : getSubProjects()) {
            processProject(support, group, subProject);
        }
    }

    private void processProject(final ReportSupport support,
                                final IReportGroupVisitor group, final MavenProject project)
            throws IOException {
        support.processProject(group, project.getArtifactId(), project,
                getIncludes(), getExcludes(), sourceEncoding);
    }

    public File getReportOutputDirectory() {
        return outputDirectory;
    }

    public void setReportOutputDirectory(final File reportOutputDirectory) {
        if (reportOutputDirectory != null && !reportOutputDirectory
                .getAbsolutePath().endsWith("jacoco-aggregate")) {
            outputDirectory = new File(reportOutputDirectory,
                    "jacoco-aggregate");
        } else {
            outputDirectory = reportOutputDirectory;
        }
    }

    public String getOutputName() {
        return "jacoco-aggregate/index";
    }

    public String getName(final Locale locale) {
        return "JaCoCo Aggregate";
    }

    protected List<MavenProject> getSubProjects() {

        List<MavenProject> result = reactorProjects;

        // need to exclude pom projects
        List<MavenProject> nonPomProjects = new ArrayList<>();
        for (MavenProject mavenProject : result) {
            if (!StringUtils.equals("pom", mavenProject.getPackaging())) {
                nonPomProjects.add(mavenProject);
            }
        }
        return nonPomProjects;
    }

}
