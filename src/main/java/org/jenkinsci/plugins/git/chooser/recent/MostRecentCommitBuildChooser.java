package org.jenkinsci.plugins.git.chooser.recent;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildChooser;
import hudson.plugins.git.util.BuildChooserContext;
import hudson.plugins.git.util.BuildChooserDescriptor;
import hudson.plugins.git.util.BuildData;
import hudson.remoting.VirtualChannel;

/**
 * A build chooser that treats the configured branches as a list of
 * alternatives.
 */
public class MostRecentCommitBuildChooser extends BuildChooser {
	@DataBoundConstructor
	public MostRecentCommitBuildChooser() {
	}

	/**
	 * Determines which revision to build
	 */
	@Override
	public Collection<Revision> getCandidateRevisions(boolean isPollCall,
	                                                  String singleBranch,
	                                                  GitClient git,
	                                                  TaskListener listener,
	                                                  BuildData data,
	                                                  BuildChooserContext context)
	                            throws GitException, IOException, InterruptedException {
		verbose(listener, "AlternativeBuildChooser.getCandidateRevisions()");
		//if the post commit hook for checking for changes does not respect ignored commit messages etc
		//we could check the commit messages ourselves using gitSCM.getExtensions()...
		//included/excluded paths might be harder
		EnvVars env = null;
		if (!isPollCall) try {
			env = context.actOnBuild(new BuildChooserContext.ContextCallable<Run<?,?>, EnvVars>() {
				public EnvVars invoke(Run<?,?> run, hudson.remoting.VirtualChannel channel) throws IOException, InterruptedException {
					return run.getEnvironment();
				}
			});
		} catch (InterruptedException x) {
			verbose(listener, "interrupted getting build variables:"+x);
		}
		Collection<Branch> remoteBranches = git.getRemoteBranches();
		List<Revision> all=new ArrayList<>();
		for (BranchSpec s : gitSCM.getBranches()) {
			BranchSpec spec = (env == null ? s : new BranchSpec(env.expand(s.getName())));
			verbose(listener, "Checking branch spec: {0}", spec);
			all.addAll(findRevisions(spec, git, remoteBranches, listener, data, context));
		}
		verbose(listener, "Select latest commit from {0}", all);
		Revision result = getLatest(all, git);
		if(result!=null) {
			verbose(listener, "Selected vevision {0}", result);
			return Collections.singleton(result);
		}
		verbose(listener, "No branch specs matched");
		return Collections.emptyList();
	}

	private Revision getLatest(Collection<Revision> revisions, GitClient git) throws InterruptedException {
		try {
			return git.withRepository((Repository repo, VirtualChannel channel) -> {
				Revision revision = null;
				int revisionCommitTime = Integer.MIN_VALUE;
				try (RevWalk walk = new RevWalk(repo)) {
					walk.setRetainBody(false);

					for (Revision r : revisions) {
						walk.reset();
						RevCommit head = walk.parseCommit(r.getSha1());
						int headCommitTime = head.getCommitTime();
						if (revision == null || headCommitTime > revisionCommitTime) {
							revisionCommitTime = headCommitTime;
							revision = r;
						}
					}
				}
				return revision;
			});
		} catch (IOException e) {
			throw new GitException("Error computing merge base", e);
		}
	}

	private List<Revision> findRevisions(BranchSpec spec, GitClient git,
	                              Collection<Branch> remoteBranches,
	                              TaskListener listener,
	                              BuildData data, BuildChooserContext context)
	                 throws GitException, IOException, InterruptedException {
		List<Revision> result = new ArrayList<>();
		ObjectId sha1;
		Revision r;
		if (spec.getName().matches("[0-9a-f]{6,40}")) {
			// might be a SHA1; strange usage, but we'll allow it
			try {
				sha1 = git.revParse(spec.getName());
				r = new Revision(sha1);
				r.getBranches().add(new Branch("detached", sha1));
				result.add(r);
				verbose(listener, "Found candidate SHA1: {0}", r);
			} catch (GitException x) {
				// ignore and look for a branch instead
			}
		} else if (!spec.getName().matches(".*[/*].*")) {
			// might be a tag name
			Set<String> tags = git.getTagNames(spec.getName());
			for (String tag : tags) {
				sha1 = git.revParse(tag);
				r = new Revision(sha1);
				r.getBranches().add(new Branch(spec.getName(), sha1));
				result.add(r);
				verbose(listener, "Found candidate tag: {0}", r);
			}
		}

		// get all matching branches
		List<Branch> branches = spec.filterMatchingBranches(remoteBranches);
		for (Branch branch : branches) {
			r = new Revision(branch.getSHA1());
			r.getBranches().add(branch);
			result.add(r);
			verbose(listener, "Found candidate branch: {0}", r);
		}
		return result;
	}

	/**
	 * Write the message to the listener only when the verbose mode is on.
	 */
	private void verbose(TaskListener listener, String format, Object... args) {
		if (GitSCM.VERBOSE) {
			listener.getLogger().println(MessageFormat.format(format,args));
		}
	}

	@Extension
	public static final class DescriptorImpl
	                          extends BuildChooserDescriptor {
		@Override
		public String getDisplayName() {
			return "Most recent commit";
		}
	}

}
