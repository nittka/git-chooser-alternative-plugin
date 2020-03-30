package org.jenkinsci.plugins.git.chooser.alternative;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.AbstractGitTestCase;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

/**
 * Test a GitSCM based project with the Alternative build chooser
 *
 * Create a test repo with a couple of branches, and a build project
 * with a list of branch specs that do or do not exist, and check
 * that the correct branch is built.
 */

public class AlternativeBuildChooserTest extends AbstractGitTestCase {
	final String commitFile1 = "commitFile1";
	final String commitFile2 = "commitFile2";

	@Test
	public void testAlternativeMaster() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("branch-doesnotexist"),
			new BranchSpec("master")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile1);
	}

	@Test
	public void testAlternativeBranch() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("branch-doesnotexist"),
			new BranchSpec("branch-exist"),
			new BranchSpec("master")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile2);
	}

	@Test
	public void testAlternativeWildcard() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("branch-doesnotexist"),
			new BranchSpec("branch-*"),
			new BranchSpec("master")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile2);
	}

	@Test
	public void testAlternativeVar() throws Exception {
		DumbSlave agent = rule.createSlave();
		setVariables(agent, new Entry("VAR_BRANCH", "exist"));
		FreeStyleProject project = setupProject(
				Arrays.asList(new BranchSpec("branch-${VAR_BRANCH}"), new BranchSpec("master")));
		project.setAssignedLabel(agent.getSelfLabel());

		initRepo();
		build(project, Result.SUCCESS, commitFile2);
	}

	protected FreeStyleProject setupProject(List<BranchSpec> specs) throws Exception {
		FreeStyleProject project = setupProject(specs, false, null, null, null, null, false, null);
		assertNotNull("could not init project", project);

		AlternativeBuildChooser chooser = new AlternativeBuildChooser();
		chooser.gitSCM = (GitSCM)project.getScm();
		chooser.gitSCM.setBuildChooser(chooser);
		return project;
	}

	protected void initRepo() throws Exception {
		commit(commitFile1, johnDoe, "Commit number 1");
		git.checkout("HEAD", "branch-exist");
		commit(commitFile2, janeDoe, "Commit number 2");
		createRemoteRepositories();
	}
}
