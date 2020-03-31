package org.jenkinsci.plugins.git.chooser.recent;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.git.chooser.recent.MostRecentCommitBuildChooser;
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

public class MostCommitBuildChooserTest extends AbstractGitTestCase {
	final String commitFile1 = "commitFile1";
	final String commitFile2 = "commitFile2";
	final String commitFile3 = "commitFile3";

	@Test
	public void masterAndNonExistingBranch() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("branch-doesnotexist"),
			new BranchSpec("master")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile1);
	}

	@Test
	public void masterAndBranch() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("master"),
			new BranchSpec("branch-1")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile2);
	}

	@Test
	public void all() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("**")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile3);
	}

	@Test
	public void allBranches() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("branch-*")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile3);
	}

	@Test
	public void nonMaster() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec(":^(?!origin/master$).*")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile3);
	}

	@Test
	public void testAlternativeWildcard() throws Exception {
		FreeStyleProject project = setupProject(Arrays.asList(
			new BranchSpec("master"),
			new BranchSpec("branch-*")
		));
		
		initRepo();
		build(project, Result.SUCCESS, commitFile3);
	}

	@Test
	public void testAlternativeVar() throws Exception {
		DumbSlave agent = rule.createSlave();
		setVariables(agent, new Entry("BRANCH", "branch-1"));
		FreeStyleProject project = setupProject(
				Arrays.asList(new BranchSpec("${BRANCH}")));
		project.setAssignedLabel(agent.getSelfLabel());

		initRepo();
		build(project, Result.SUCCESS, commitFile2);
	}



	protected FreeStyleProject setupProject(List<BranchSpec> specs) throws Exception {
		FreeStyleProject project = setupProject(specs, false, null, null, null, null, false, null);
		assertNotNull("could not init project", project);

		MostRecentCommitBuildChooser chooser = new MostRecentCommitBuildChooser();
		chooser.gitSCM = (GitSCM)project.getScm();
		chooser.gitSCM.setBuildChooser(chooser);
		return project;
	}

	//sleep is necessary as the commit time resolution seems not to be high enough
	protected void initRepo() throws Exception {
		commit(commitFile1, johnDoe, "Commit number 1");
		git.checkout("HEAD", "branch-1");
		Thread.sleep(1000);
		commit(commitFile2, janeDoe, "Commit number 2");
		git.checkout("master");
		git.checkout("HEAD", "branch-2");
		Thread.sleep(1000);
		commit(commitFile3, johnDoe, "Commit number 3");
		createRemoteRepositories();
	}
}
