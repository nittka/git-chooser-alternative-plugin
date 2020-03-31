package org.jenkinsci.plugins.git.chooser.recent;

import org.junit.Assert;
import org.junit.Test;

import hudson.plugins.git.BranchSpec;

//just make sure we understand the branch spec correctly
public class BranchSpecMatchTest {

	@Test
	public void wildCard() {
		BranchSpec spec = new BranchSpec("**");
		match(spec, "origin/master");
		match(spec, "origin/branch");
		match(spec, "origin/feature/branch");
	}

	@Test
	public void master() {
		BranchSpec spec = new BranchSpec("master");
		match(spec, "origin/master");

		noMatch(spec, "origin/branch");
		noMatch(spec, "origin/feature/branch");
	}

	@Test
	public void master2() {
		BranchSpec spec = new BranchSpec("origin/master");
		match(spec, "origin/master");

		noMatch(spec, "origin/branch");
		noMatch(spec, "origin/feature/branch");
	}

	@Test
	public void branch() {
		BranchSpec spec = new BranchSpec("branch*");
		match(spec, "origin/branch");
		match(spec, "origin/branch1");
		match(spec, "origin/branch2");

		noMatch(spec, "origin/master");
		noMatch(spec, "origin/feature/branch");
	}

	@Test
	public void nonMaster() {
		BranchSpec spec = new BranchSpec(":^(?!origin/master$|origin/develop$).*");
		match(spec, "origin/feature/branch");
		match(spec, "origin/branch");

		noMatch(spec, "origin/master");
		noMatch(spec, "origin/develop");
	}

	private void match(BranchSpec spec, String branchName) {
		Assert.assertTrue(branchName +" expected to match "+spec, spec.matches(branchName));
	}

	private void noMatch(BranchSpec spec, String branchName) {
		Assert.assertFalse(branchName +" not expected to match "+spec, spec.matches(branchName));
	}
}
