/*
 * August 25, 2020 Fixed hanging solver due to buffer not read
 * (Copyright for this change in method startSync (c) Siemens Aktiengesellschaft Oesterreich, 2020)
 *
 * SPDX-License-Identifier: MIT
 */
package it.unical.mat.embasp.platforms.desktop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import it.unical.mat.embasp.base.Callback;
import it.unical.mat.embasp.base.InputProgram;
import it.unical.mat.embasp.base.OptionDescriptor;
import it.unical.mat.embasp.base.Output;
import it.unical.mat.embasp.base.Service;

/**
 * is a specialization for a Desktop platform
 *
 * @see Service
 */

public abstract class DesktopService implements Service {
	/** Stores solver's executable path */
	protected String exe_path;
	protected String load_from_STDIN_option;

	public DesktopService(final String exe_path) {
		this.exe_path = exe_path;
	}

	public String getExePath() {
		return exe_path;
	}

	abstract protected Output getOutput(String output, String error);

	/**
	 * set {@link #exe_path} to a new path*
	 *
	 * @param exe_path
	 *            a string representing the path for the new solver
	 */
	public void setExePath(final String exe_path) {
		this.exe_path = exe_path;
	}

	/**
	 * Start a new process for the {@link #exe_path} and starts solving
	 *
	 * @see it.unical.mat.embasp.base.Service#startAsync(Callback, List, List)
	 */
	@Override
	public void startAsync(final Callback callback, final List<InputProgram> programs, final List<OptionDescriptor> options) {

		new Thread() {
			@Override
			public void run() {
				callback.callback(startSync(programs, options));
			}
		}.start();

	}

	/**
	 * Start a new process for the {@link #exe_path} and starts solving
	 *
	 * @see it.unical.mat.embasp.base.Service#startSync(List, List)
	 */
	@Override
	public Output startSync(final List<InputProgram> programs, final List<OptionDescriptor> options) {

		List<String> exe_options = new ArrayList<>();
		for (final OptionDescriptor o : options) {
			if (o != null) {
				exe_options.add(o.getOption());
			} else
				System.err.println("Warning : wrong " + OptionDescriptor.class.getName());
		}

		List<String> files_paths = new ArrayList<>();
		String final_program = new String();

		for (final InputProgram p : programs)
			if (p != null) {
				final_program += p.getPrograms();
				for(final String program_file: p.getFilesPaths()){
					File f = new File(program_file);
					if(f.exists() && !f.isDirectory()) { 
						files_paths.add(program_file);
					}
					else
						System.err.println("Warning : the file " + f.getAbsolutePath() + " does not exists.");
				}
			} else
				System.err.println("Warning : wrong " + InputProgram.class.getName());

		final StringBuffer solverOutput = new StringBuffer();
		final StringBuffer solverError = new StringBuffer();

		try {

			final long startTime = System.nanoTime();
			
			if (exe_path == null)
				return new Output("", "Error: executable not found");

			List<String> command = new ArrayList<>();
			command.add(exe_path);
			command.addAll(exe_options);
			command.addAll(files_paths);

			if (!final_program.isEmpty()){
				command.add(this.load_from_STDIN_option);
			}

			System.err.println(command);

			final Process solver_process = new ProcessBuilder(command).start();
			
			Thread threadOutput=new Thread() {
				@Override
				public void run() {
					try {

						final BufferedReader bufferedReaderOutput = new BufferedReader(new InputStreamReader(solver_process.getInputStream()));

						// Read output of the solver and store in solverOutput
						String currentLine;
						while ((currentLine = bufferedReaderOutput.readLine()) != null)
							solverOutput.append(currentLine + "\n");
					} catch (final IOException e) {
						e.printStackTrace();
					}

				}
			};
			threadOutput.start();
			
			Thread threadError = new Thread() {
				@Override
				public void run() {
					try {

						final BufferedReader bufferedReaderError = new BufferedReader(new InputStreamReader(solver_process.getErrorStream()));

						String currentErrLine;
						while ((currentErrLine = bufferedReaderError.readLine()) != null)
							solverError.append(currentErrLine + "\n");

					} catch (final IOException e) {
						e.printStackTrace();
					}

				}
			};
			threadError.start();

			if(!final_program.isEmpty()) {
				final PrintWriter writer = new PrintWriter(solver_process.getOutputStream());
				writer.println(final_program);
				if (writer != null)
					writer.close();
				solver_process.waitFor();
			}

			threadOutput.join();
			threadError.join();

			final long stopTime = System.nanoTime();
			System.err.println("Total time : " + (stopTime - startTime));
			
			return getOutput(solverOutput.toString(), solverError.toString());

		} catch (final IOException e2) {
			e2.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		return getOutput("", "");

	}

}
