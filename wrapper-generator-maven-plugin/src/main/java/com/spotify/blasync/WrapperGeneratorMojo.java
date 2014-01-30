package com.spotify.blasync;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.sun.codemodel.JCodeModel;

/**
 * @goal generate
 * @execute phase="generate-sources"
 */
public class WrapperGeneratorMojo extends AbstractMojo {

	/**
	 * @parameter
	 */
	private List<String> classNames;

	/**
	 * @parameter
	 */
	private File destDir;

	public List<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(List<String> classNames) {
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

		List<String> classNames = getClassNames();
		if (classNames == null) {
			throw new MojoExecutionException("classNames is not set");
		}

		File destDir = getDestDir();
		if (destDir == null) {
			throw new MojoExecutionException("destDir is not set");
		}

		try {

			LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);
				classes.add(clazz);
			}

			JCodeModel model = new JCodeModel();
			WrapperGenerator generator = new WrapperGenerator();
			generator.generate(model, classes);

			destDir.mkdirs();
			model.build(destDir, (PrintStream) null);

		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
