package org.jenkinsci.plugins.git.chooser.recent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.git.GitException;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildChooserContext;
import hudson.plugins.git.util.BuildChooserDescriptor;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.CommitTimeComparator;
import hudson.plugins.git.util.DefaultBuildChooser;
import hudson.remoting.VirtualChannel;

/**
 * A build chooser that treats the configured branches as a list of alternatives.
 */
public class MostRecentCommitBuildChooser extends DefaultBuildChooser {
	@DataBoundConstructor
	public MostRecentCommitBuildChooser() {
	}

	@Override
	public Collection<Revision> getCandidateRevisions(boolean isPollCall, String branchSpec, GitClient git,
		TaskListener listener, BuildData data, BuildChooserContext context)
		throws GitException, IOException, InterruptedException {
		Collection<Revision> defaultCandidates = super.getCandidateRevisions(isPollCall, branchSpec, git, listener, data,
			context);
		if (defaultCandidates == null || defaultCandidates.isEmpty()) {
			return defaultCandidates;
		} else {
			final List<Revision> possiblyUnsorted = new ArrayList<>(defaultCandidates);
			return git.withRepository((Repository repo, VirtualChannel channel) -> {
				Collections.sort(possiblyUnsorted, new CommitTimeComparator(repo));
				Revision last = possiblyUnsorted.get(possiblyUnsorted.size() - 1);
				return Collections.singleton(last);
			});
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
