package org.jenkinsci.plugins.git.chooser.alternative;

import hudson.Extension;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.plugins.git.*;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.BuildChooser;
import hudson.plugins.git.util.BuildChooserContext;
import hudson.plugins.git.util.BuildChooserDescriptor;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * A build chooser that treats the configured branches as a list of
 * alternatives.
 */
public class AlternativeBuildChooser extends BuildChooser {
	@DataBoundConstructor
	public AlternativeBuildChooser() {
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
		for (BranchSpec s : gitSCM.getBranches()) {
			BranchSpec spec = (env == null ? s : new BranchSpec(env.expand(s.getName())));
			verbose(listener, "Checking branch spec: {0}", spec);
			Revision r = findRevision(spec, git, remoteBranches, listener, data, context);
			if (r != null) return Collections.singletonList(r);
		}
		verbose(listener, "No branch specs matched");
		return Collections.emptyList();
	}

	private Revision findRevision(BranchSpec spec, GitClient git,
	                              Collection<Branch> remoteBranches,
	                              TaskListener listener,
	                              BuildData data, BuildChooserContext context)
	                 throws GitException, IOException, InterruptedException {
		Revision r = null;
		ObjectId sha1;
		if (spec.getName().matches("[0-9a-f]{6,40}")) {
			// might be a SHA1; strange usage, but we'll allow it
			try {
				sha1 = git.revParse(spec.getName());
				r = new Revision(sha1);
				r.getBranches().add(new Branch("detached", sha1));
				verbose(listener, "Found SHA1: {0}", r);
			} catch (GitException x) {
				// ignore and look for a branch instead
			}
		} else if (!spec.getName().matches(".*[/*].*")) {
			// might be a tag name
			Set<String> tags = git.getTagNames(spec.getName());
			if (!tags.isEmpty()) {
				sha1 = git.revParse(tags.iterator().next());
				r = new Revision(sha1);
				r.getBranches().add(new Branch(spec.getName(), sha1));
				verbose(listener, "Found tag: {0}", r);
			}
		}
		if (r != null) return r;

		// get all matching branches
		List<Branch> branches = spec.filterMatchingBranches(remoteBranches);
		if (!branches.isEmpty()) {
			Branch b = branches.get(0);
			r = new Revision(b.getSHA1());
			r.getBranches().add(b);
			verbose(listener, "Found branch: {0}", r);
		}
		return r;
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
			return "Alternative";
		}
	}

}
