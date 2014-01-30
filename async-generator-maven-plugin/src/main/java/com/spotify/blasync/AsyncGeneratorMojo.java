package com.spotify.blasync;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.sun.codemodel.JCodeModel;

/**
 * @goal generate
 * @execute phase="generate-sources"
 */
public class AsyncGeneratorMojo extends AbstractMojo {

	/**
	 * @parameter
	 */
	private Map<String, String> classNames;

	/**
	 * @parameter
	 */
	private File destDir;

	public Map<String, String> getClassNames() {
		return classNames;
	}

	public void setClassNames(Map<String, String> classNames) {
		this.classNames = classNames;
	}

	public File getDestDir() {
		return destDir;
	}

	public void setDestDir(File destDir) {
		this.destDir = destDir;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		Map<String, String> classNames = getClassNames();
		if (classNames == null) {
			throw new MojoExecutionException("classNames is not set");
		}

		File destDir = getDestDir();
		if (destDir == null) {
			throw new MojoExecutionException("destDir is not set");
		}

		try {

			Map<Class<?>, String> classes = new HashMap<Class<?>, String>();
			for (Entry<String, String> entry : classNames.entrySet()) {
				Class<?> clazz = Class.forName(entry.getKey());
				classes.put(clazz, entry.getValue());
			}

			JCodeModel model = new JCodeModel();
			AsyncGenerator generator = new AsyncGenerator();
			generator.generate(model, classes);

			destDir.mkdirs();
			model.build(destDir, (PrintStream) null);

		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
