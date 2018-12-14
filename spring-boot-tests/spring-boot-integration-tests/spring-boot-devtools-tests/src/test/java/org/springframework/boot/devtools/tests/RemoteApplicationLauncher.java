/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.boot.devtools.RemoteSpringApplication;
import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link ApplicationLauncher} implementations that use
 * {@link RemoteSpringApplication}.
 *
 * @author Andy Wilkinson
 */
abstract class RemoteApplicationLauncher extends AbstractApplicationLauncher {

	RemoteApplicationLauncher(Directories directories) {
		super(directories);
	}

	@Override
	public LaunchedApplication launchApplication(JvmLauncher javaLauncher,
			File serverPortFile) throws Exception {
		LaunchedJvm applicationJvm = javaLauncher.launch("app",
				createApplicationClassPath(), "com.example.DevToolsTestApplication",
				serverPortFile.getAbsolutePath(), "--server.port=0",
				"--spring.devtools.remote.secret=secret");
		int port = awaitServerPort(applicationJvm.getStandardOut(), serverPortFile);
		BiFunction<Integer, File, Process> remoteRestarter = getRemoteRestarter(
				javaLauncher);
		return new LaunchedApplication(getDirectories().getRemoteAppDirectory(),
				applicationJvm.getStandardOut(), applicationJvm.getStandardError(),
				applicationJvm.getProcess(), remoteRestarter.apply(port, null),
				remoteRestarter);
	}

	private BiFunction<Integer, File, Process> getRemoteRestarter(
			JvmLauncher javaLauncher) {
		return (port, classesDirectory) -> {
			try {
				LaunchedJvm remoteSpringApplicationJvm = javaLauncher.launch(
						"remote-spring-application",
						createRemoteSpringApplicationClassPath(classesDirectory),
						RemoteSpringApplication.class.getName(),
						"--spring.devtools.remote.secret=secret",
						"http://localhost:" + port);
				awaitRemoteSpringApplication(remoteSpringApplicationJvm.getStandardOut());
				return remoteSpringApplicationJvm.getProcess();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		};
	}

	protected abstract String createApplicationClassPath() throws Exception;

	private String createRemoteSpringApplicationClassPath(File classesDirectory)
			throws Exception {
		File remoteAppDirectory = getDirectories().getRemoteAppDirectory();
		if (classesDirectory == null) {
			copyApplicationTo(remoteAppDirectory);
		}
		List<String> entries = new ArrayList<>();
		entries.add(remoteAppDirectory.getAbsolutePath());
		entries.addAll(getDependencyJarPaths());
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	private int awaitServerPort(File standardOut, File serverPortFile) throws Exception {
		long end = System.currentTimeMillis() + 30000;
		while (serverPortFile.length() == 0) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(String.format(
						"server.port file was not written within 30 seconds. "
								+ "Application output:%n%s",
						FileCopyUtils.copyToString(new FileReader(standardOut))));
			}
			Thread.sleep(100);
		}
		FileReader portReader = new FileReader(serverPortFile);
		int port = Integer.valueOf(FileCopyUtils.copyToString(portReader));
		return port;
	}

	private void awaitRemoteSpringApplication(File standardOut) throws Exception {
		long end = System.currentTimeMillis() + 30000;
		while (!standardOut.exists()) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(
						"Standard out file was not written " + "within 30 seconds");
			}
			Thread.sleep(100);
		}
		while (!FileCopyUtils.copyToString(new FileReader(standardOut))
				.contains("Started RemoteSpringApplication")) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(
						"RemoteSpringApplication did not start within 30 seconds");
			}
			Thread.sleep(100);
		}
	}

}
