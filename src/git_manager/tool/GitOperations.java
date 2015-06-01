package git_manager.tool;

import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import processing.app.Base;
import processing.app.Editor;

public class GitOperations {

  public static String GITIGNORE_LOCATION = "/data/code/sample_gitignore.txt";
	Editor editor;
	File gitDir;
	File thisDir;
	Git git;
	String uName, pass, remote;

	public GitOperations(Editor editor) {
		this.editor = editor;
		if (Base.isLinux())
			gitDir = new File(editor.getSketch().getFolder().getAbsolutePath()
					+ "/.git");
		else
			gitDir = new File(editor.getSketch().getFolder().getAbsolutePath()
					+ "\\.git");
		thisDir = new File(editor.getSketch().getFolder().getAbsolutePath());
		try {
			git = new Git(new FileRepository(gitDir));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void initRepo() {
		if (gitDir.exists()) {
			System.out.println("Repo already exists!");
			return;
		}

		try {
			System.out.println(gitDir.getAbsolutePath());
			Git.init().setDirectory(thisDir).setBare(false).call();
			System.out.println("New repo created.");
			createGitIgnore();
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		} catch (TransportException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}
	
	// TODO: Ensure that repo name == sketchname
	public void createGitIgnore() {
	  InputStream sourceGitignore = this.getClass().getResourceAsStream(GITIGNORE_LOCATION);
	  File destGitignore = new File(thisDir.getAbsolutePath() + "\\.gitignore");
	  try {
      Files.copy(sourceGitignore, destGitignore.toPath());
    } catch (IOException e) {
      e.printStackTrace();
    }
	}

	public void initBareRepo() {
		Repository repo;
		try {
			repo = new FileRepository(thisDir);
			repo.create(true);
			System.out.println("Bare repo created.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void cloneRepo(File cloneFrom, File cloneTo) {
		try {
			Git.cloneRepository().setURI(cloneFrom.getAbsolutePath())
					.setDirectory(cloneTo).setBranch("master").setBare(false)
					.setRemote("origin").setNoCheckout(false).call();
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		} catch (TransportException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void addAndCommit(String comment) {

		if (comment == null) {
			System.out.println("Commit Cancelled");
			return;
		}
		if (comment.equals("")) {
			int result = JOptionPane.showConfirmDialog(null,
					"You have not entered a commit message. Proceed anyway?",
					"No commit message warning", JOptionPane.OK_CANCEL_OPTION);

			if (result != JOptionPane.OK_OPTION) {
				System.out.println("Commit Cancelled");
				return;
			}
		}

		try {
			git.add().addFilepattern(".").call();
			git.commit().setMessage(comment).call();
			System.out.println("Snapshot complete");
		} catch (NoFilepatternException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void addFiles() {

		try {
			git.add().addFilepattern(".").call();
			System.out.println("All files added");
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void commitChanges(String comment) {

		if (comment == null) {
			System.out.println("Commit Cancelled");
			return;
		}
		if (comment.equals("")) {
			int result = JOptionPane.showConfirmDialog(null,
					"You have not entered a commit message. Proceed anyway?",
					"No commit message warning", JOptionPane.OK_CANCEL_OPTION);

			if (result != JOptionPane.OK_OPTION) {
				System.out.println("Commit Cancelled");
				return;
			}
		}
		try {
			git.commit().setMessage(comment).call();
			System.out.println("Commit complete");
		} catch (NoHeadException e) {
			e.printStackTrace();
		} catch (NoMessageException e) {
			e.printStackTrace();
		} catch (UnmergedPathsException e) {
			e.printStackTrace();
		} catch (ConcurrentRefUpdateException e) {
			e.printStackTrace();
		} catch (WrongRepositoryStateException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void pushToRemote() {

		if (uName == null || pass == null || remote == null) {
			System.out.println("Push cancelled");
			return;
		}
		if (uName.equals("") || pass.equals("") || remote.equals("")) {
			if (uName.equals(""))
				System.out.println("Please enter a valid username");

			if (pass.equals(""))
				System.out.println("Please enter a valid password");

			if (remote.equals(""))
				System.out
						.println("Please enter a valid online repository address");
			uName = pass = remote = null;
			return;
		}

		// Get TransportException when project is >1Mb
		// TODO: Fix this:
		// https://groups.google.com/forum/#!topic/bitbucket-users/OUsa8sb_Ti4
		CredentialsProvider cp = new UsernamePasswordCredentialsProvider(uName,
				pass);

		Iterable<PushResult> pc;
		try {
			pc = git.push().setRemote(remote).setCredentialsProvider(cp).call();
			for (PushResult pushResult : pc) {
				System.out.println(pushResult.getURI());
			}
			System.out.println("Push Complete");

		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		} catch (TransportException e) {
			// System.out
			// .println("Please use a project of size <1MB when pushing (will be resolved soon)...");
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		} finally {
			uName = null;
			remote = null;
			pass = null;
		}

	}

	void getUnameandPass() {
		JTextField uName = new JTextField(15);
		JPasswordField pass = new JPasswordField(15);
		JTextField remote = new JTextField(15);

		JPanel panel = new JPanel(new GridLayout(0, 1));
		JPanel un = new JPanel();
		JPanel un2 = new JPanel();
		JPanel un3 = new JPanel();
		un.add(new JLabel("Username:   "));
		un.add(uName);
		panel.add(un);
		un2.add(new JLabel("Password:   "));
		un2.add(pass);
		panel.add(un2);
		un3.add(new JLabel("GitHub repo:"));
		un3.add(remote);
		panel.add(un3);

		int result = JOptionPane.showConfirmDialog(null, panel,
				"Login to GitHub", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			this.uName = uName.getText();
			this.pass = new String(pass.getPassword());
			this.remote = remote.getText();
		}
	}
	
	 public void pullFromRemote() {
	    PullResult result;
	    try {
	      result = git.pull().call();
	      FetchResult fetchResult = result.getFetchResult();
	      MergeResult mergeResult = result.getMergeResult();
	      // TODO: Handle merge conflicts :O
	      // TODO: Handle NoHeadException (repo not initialized)
	      // TODO: Handle InvalidConfigurationException (no pushes made till now- 
	      //       configure remote manually)
	      System.out.println(mergeResult.getMergeStatus());
//	      System.out.println("fetch status: " + fetchResult.getMessages());
	    } catch (InvalidRemoteException e) {
	      e.printStackTrace();
	    } catch (TransportException e) {
	      // System.out
	      // .println("Please use a project of size <1MB when pushing (will be resolved soon)...");
	      e.printStackTrace();
	    } catch (GitAPIException e) {
	      e.printStackTrace();
	    }
	  }
	 
	 public void displayStatus() {
	   Status status;
    try {
      status = git.status().call();
      if (status.isClean()) {
        System.out.println("No changes made in sketch since last commit");
      }
      else {
        Set<String> conflicts = status.getConflicting();
        Set<String> untracked = status.getUntracked();
        Set<String> added = status.getAdded();
        Set<String> changed = status.getChanged();
        Set<String> deleted = status.getRemoved();
//        deleted.addAll(status.getMissing());
        Set<String> missing = status.getMissing();
        Set<String> modified = status.getModified();
        
        // TODO: Explanations not technically sound- intended for first time users not fully familiar with git
        printFileStatus("Added", "Files not present in the last commit/snapshot, but will be added in the next commit", added);
        printFileStatus("Changed", "Files present in the last commit/snapshot whose modifications will added in the next commit", changed);
        printFileStatus("Deleted", "File whose deletion will be recorded in the next commit", deleted);
        printFileStatus("Missing", "File whose deletion will be not recorded in the next commit", missing);
        printFileStatus("Modified", "File present in the last snapshot/commit, subsequently modified, whose modifications will not yet be added in the next commit", modified);
        printFileStatus("Untracked", "New files not yet present in any commits", untracked);
        
        if (!conflicts.isEmpty()) {
          System.out.println("Warning: Conflicts detected in the following files");
          printFileStatus("", "", conflicts);
        }
      }
    } catch (NoWorkTreeException e) {
      e.printStackTrace();
    } catch (GitAPIException e) {
      e.printStackTrace();
    }
	   
	 }
	 
	 void printFileStatus(String type, String description, Set<String> files) {
	   if (!files.isEmpty()) {
	     if (type != null && !type.isEmpty()) {
	       System.out.println("\n" + type + " Files (" + description + "):");
	     }
	     for (String f : files) {
	       System.out.println("\t" + f);
	     }
	   }
	 }
	 
	 void resetHard() {
	   ResetCommand reset = git.reset();
	   reset.setRef(Constants.HEAD);
//	   reset.addPath(".");
	   reset.setMode(ResetType.HARD);
	   try {
      reset.call();
      System.out.println("Hard reset completed. Sketch is now exactly like the last snapshot/commit.");
    } catch (CheckoutConflictException e) {
      e.printStackTrace();
    } catch (GitAPIException e) {
      e.printStackTrace();
    }
	 }
}
