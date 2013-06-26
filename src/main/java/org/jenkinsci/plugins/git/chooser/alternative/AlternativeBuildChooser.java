package org.jenkinsci.plugins.git.chooser.alternative;

import hudson.Extension;
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
	                            throws GitException, IOException {
		throw new UnsupportedOperationException("Not yet implemented");
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
