/****************************************************************************
**
** Copyright (C) 1992-2009 Nokia. All rights reserved.
** Copyright (C) 2009-2022 Dr. Peter Droste, Omix Visualization GmbH & Co. KG. All rights reserved.
**
** This file is part of Qt Jambi.
**
** ** $BEGIN_LICENSE$
** GNU Lesser General Public License Usage
** Alternatively, this file may be used under the terms of the GNU Lesser
** General Public License version 2.1 as published by the Free Software
** Foundation and appearing in the file LICENSE.LGPL included in the
** packaging of this file.  Please review the following information to
** ensure the GNU Lesser General Public License version 2.1 requirements
** will be met: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html.
** 
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3.0 as published by the Free Software
** Foundation and appearing in the file LICENSE.GPL included in the
** packaging of this file.  Please review the following information to
** ensure the GNU General Public License version 3.0 requirements will be
** met: http://www.gnu.org/copyleft/gpl.html.
** $END_LICENSE$
**
** This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
** WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
**
****************************************************************************/

package io.qt.autotests;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;

import io.qt.QNoNativeResourcesException;
import io.qt.QtObjectInterface;
import io.qt.core.QCoreApplication;
import io.qt.core.QEvent;
import io.qt.core.QIODevice;
import io.qt.core.QObject;
import io.qt.core.QProcess;
import io.qt.core.QProcessEnvironment;
import io.qt.core.QResource;
import io.qt.core.QStringList;
import io.qt.core.QThread;
import io.qt.gui.QGuiApplication;
import io.qt.internal.QtJambiDebugTools;
import io.qt.internal.QtJambiInternal;
import io.qt.widgets.QApplication;

public abstract class ApplicationInitializer extends UnitTestInitializer{
	
	protected static final List<WeakReference<QtObjectInterface>> instances = Collections.synchronizedList(new ArrayList<>());
	
    @BeforeClass
	public static void testInitialize() throws Exception {
    	testInitialize(0);
    }
    
    public static void testInitializeWithGui() throws Exception {
    	testInitialize(1);
    }
    
    public static void testInitializeWithWidgets() throws Exception {
    	testInitialize(2);
    }
    
    private static void testInitialize(int mode) throws Exception {
        try {
			if(QCoreApplication.instance()==null) {
				java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testInitialize: begin");
				QResource.addClassPath(".");
				QCoreApplication.setApplicationName("QtJambiUnitTest");
				switch(mode){
				case 0:
					QCoreApplication.initialize(new String[0]); 
					break;
				case 1:
					QGuiApplication.initialize(new String[0]);
					break;
					default:
					QApplication.initialize(new String[0]);
					break;
				}
		        QThread.currentThread().setObjectName("main");
			    java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testInitialize: done");
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
    }

    @AfterClass
    public static void testDispose() throws Exception {
    	try {
    		Object currentThread = QThread.currentThread();
    		while(!instances.isEmpty()) {
    			WeakReference<QtObjectInterface> weak = instances.remove(0);
    			QtObjectInterface o = weak.get();
    			if(o!=null && QtJambiInternal.ownership(o)==QtJambiInternal.Ownership.Cpp && !o.isDisposed()) {
    				if(o instanceof QObject && ((QObject) o).thread()!=currentThread) {
    					((QObject) o).disposeLater();
    				}else {
    					o.dispose();
    				}
    			}
    		}
	        java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: BEGIN");
	        System.gc();
	        System.runFinalization();
	
	        // We are attempting to reach a point here where memory should be reclaimed
	        // except for anything linked to QApplication.
	
	        QCoreApplication app = QCoreApplication.instance();
	        try {
	            java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: QCoreApplication-only  app="+app);
	        } catch(QNoNativeResourcesException e) {
	            java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: QCoreApplication-only  io.qt.QNoNativeResourcesException: app="+e.getMessage());
	        }
	
	        System.err.flush();
	        System.out.flush();
	
	        if(app != null) {
	            try {
	        		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: processEvents() 1 PRE");
	                QCoreApplication.processEvents();	// NPE
	        		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: processEvents() 1 POST");
	            } catch(Throwable t) {
	                t.printStackTrace();
	            }
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: sendPostedEvents(DeferredDelete) PRE");
	            QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: sendPostedEvents(DeferredDelete) POST");
	        }
	        app = QCoreApplication.instance();
	        if(app != null) {
				java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: garbage PRE");
		        for (int i = 0; i < 4; i++) {
		        	System.gc();
		            System.runFinalization();
		            Thread.sleep(50);
		            QCoreApplication.processEvents();
		            QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());
		        	System.gc();
		            System.runFinalization();
				}
		        while(QtJambiDebugTools.hasDeleteLaterEvents()) {
		            QCoreApplication.processEvents();
		            QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());	        	
		        }
				java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: garbage POST");
		        
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: processEvents() 2 PRE");
	            QCoreApplication.processEvents();
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: processEvents() 2 POST");
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: processEvents() 3 PRE");
	            QCoreApplication.processEvents(/*QEventLoop.ProcessEventsFlag.DeferredDeletion*/);
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: processEvents() 3 POST");
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: QCoreApplication.dispose() PRE");
	            app.dispose();
	    		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: QCoreApplication.dispose() POST");
	            try {
	            	if(app.isDisposed())
	            		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: done  app="+app.getClass()+"@"+app.hashCode());
	            	else
	            		java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: not done  app="+app);
	            } catch(QNoNativeResourcesException e) {
	                java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: done  io.qt.QNoNativeResourcesException: app="+e.getMessage());
	            }
	            app = null;		// kill hard-reference
	        }
			java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: garbage PRE");
	        System.gc();
	        System.runFinalization();
	        QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());
	        System.gc();
	        System.runFinalization();
	        QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());
	        Thread.sleep(50);
			java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: garbage POST");
	
	        // We are attempting to reach a point here where memory should be reclaimed
	        // including that which was linked to QCoreApplication
	
	        java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: shutdown PRE");
	        QCoreApplication.shutdown();
	        java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: shutdown POST");
	        
	        System.gc();
	        System.runFinalization();
	        QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());
	        Thread.sleep(50);
	        
	        QtJambiDebugTools.flushOut();  // fflush(stdout)
	        QtJambiDebugTools.flushErr();  // fflush(stderr)
	        int objectCount = QtJambiDebugTools.objectCount();  // QtJambiLink::QtJambiLink_dump()
	        QtJambiDebugTools.flushErr();  // fflush(stderr)
	        java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: end objectCount="+objectCount);
	
	        if(objectCount == 0)
	            return;  // optimization due to class loading causing some references to be set
	
	        System.gc();
	        System.runFinalization();
	        QCoreApplication.sendPostedEvents(null, QEvent.Type.DeferredDispose.value());
	        QCoreApplication.processEvents();
	        System.gc();
	        System.runFinalization();
	        Thread.sleep(50);
	
	        QtJambiDebugTools.flushOut();  // fflush(stdout)
	        QtJambiDebugTools.flushErr();  // fflush(stderr)
	        objectCount = QtJambiDebugTools.objectCount();  // QtJambiLink::QtJambiLink_dump()
	        QtJambiDebugTools.flushErr();  // fflush(stderr)
	        java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: end objectCount="+objectCount);
    	}finally {
            java.util.logging.Logger.getLogger("io.qt.autotests").log(java.util.logging.Level.FINE, "testDispose: DONE");
    	}
    }
    
	static void testDeployerApp(String applicationName, String...modules) throws InterruptedException, IOException {
    	String version = QtJambiInternal.majorVersion()+"."+QtJambiInternal.minorVersion()+"."+QtJambiInternal.qtjambiPatchVersion();
    	boolean isDebug = QtJambiInternal.isDebugBuild();
    	String jvmVersion = System.getProperty("java.version");
    	boolean java8 = jvmVersion.startsWith("1.") || jvmVersion.startsWith("8.") || jvmVersion.startsWith("7.");
    	String jambidir = System.getProperty("user.dir");
    	Assume.assumeTrue(jambidir!=null);
    	final File testsDir = new File(new File(new File(jambidir, version), "build"), "tests");
    	Assume.assumeTrue(testsDir.getAbsolutePath()+" not a directory", testsDir.isDirectory());
    	final File targetDir = new File(testsDir, "tmp_"+QtJambiInternal.processName());
    	targetDir.mkdirs();
    	Assert.assertTrue(targetDir.isDirectory());
    	String classPath;
    	String modulePath;
    	String executable = "";
    	String platform;
    	String osName = System.getProperty("os.name").toLowerCase();
    	if(osName.startsWith("windows")) {
    		if(isDebug)
    			executable += "d";
    		executable += ".exe";
    		platform = "windows";
    		switch(System.getProperty("os.arch").toLowerCase()) {
        	case "arm":
        	case "arm32":
        		platform += "-arm32"; break;
        	case "arm64":
        	case "aarch64":
        		platform += "-arm64"; break;
        	case "x86_64":
        	case "x64":
        	case "amd64":
        		platform += "-x64"; break;
    		default:
        		platform += "-x86"; break;
    		}
    	}else if(osName.startsWith("mac")) {
    		if(isDebug)
    			executable += "_debug";
    		executable += ".app";
    		platform = "macos";
    	}else if(osName.startsWith("android")) {
    		Assume.assumeFalse("Cannot run on android", true);
    		return;
    	}else {
    		if(isDebug)
    			executable += "_debug";
    		platform = "linux";
    		switch(System.getProperty("os.arch").toLowerCase()) {
        	case "arm":
        	case "arm32":
        		platform += "-arm32"; break;
        	case "arm64":
        	case "aarch64":
        		platform += "-arm64"; break;
        	case "x86_64":
        	case "x64":
        	case "amd64":
        		platform += "-x64"; break;
    		default:
        		platform += "-x86"; break;
    		}
    	}
    	String qtBinariesPath = QtJambiInternal.qtLibraryPath();
    	File deploymentDir = new File(new File(new File(new File(jambidir, version), "deployment"), "native"), platform);
    	if(isDebug) {
    		platform += "-debug";
    		deploymentDir = new File(deploymentDir, "debug");
    	}else {
    		deploymentDir = new File(deploymentDir, "release");
    	}
    	deploymentDir = new File(deploymentDir, "utilities");
    	String macosPrefix = "";
    	if(osName.startsWith("mac")) {
    		macosPrefix = "../../../";
    	}
		final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		String processName = "";
    	File jambiDeploymentDir = null;
    	final QProcess process = new QProcess();
    	try {
	    	if(java8) {
	    		classPath = macosPrefix+"../java8";
	    		classPath += File.pathSeparator+macosPrefix+"../../../deployment/qtjambi-jre8-"+version+".jar";
	    		for(String mod : modules) {
		    		classPath += File.pathSeparator+macosPrefix+"../../../deployment/qtjambi-"+mod+"-jre8-"+version+".jar";
		    		if("sql".equals(mod) && !System.getProperty("qtjambi.deployer.test.jdbc", "").isEmpty()) {
		    			String jdbcPath = targetDir.toPath().toAbsolutePath().relativize(new File(System.getProperty("qtjambi.deployer.test.jdbc")).toPath().toAbsolutePath()).toString();
			    		classPath += File.pathSeparator+macosPrefix+jdbcPath;
		    		}
	    		}
	        	io.qt.qtjambi.deployer.Main.main(new String[]{
	        			"application",
	        			"--application-name="+applicationName,
	        			"--dir="+targetDir.getAbsolutePath(),
	        			"--class-path="+classPath,
	        			"--ico=C:\\",
	        			"--main-class=io.qt.autotests."+applicationName,
	        			"--jvm-path="+System.getProperty("java.home"),
	        			"--executable="+new File(deploymentDir, "QtJambiLauncher"+executable).getAbsolutePath(),
	        			"--",
	        			"-Djava.library.path="+qtBinariesPath,
	        			"-Dio.qt.debug="+(isDebug ? "debug" : "release"),
	        			"-Djava.util.logging.config.file="+System.getProperty("java.util.logging.config.file", ""),
	        			"-Xrs",
	        			"-Dio.qt.log-messages="+System.getProperty("io.qt.log-messages", "ALL"),
	        			"-Dio.qt.debug.level="+System.getProperty("io.qt.debug.level", "1"),
	        			"-Dio.qt.verbose-loading="+System.getProperty("io.qt.verbose-loading", "false")
	            	});
	    	}else {
	    		classPath = macosPrefix+"../java";
	    		modulePath = "../"+macosPrefix+"../../deployment/qtjambi-"+version+".jar";
	    		for(String mod : modules) {
		    		modulePath += File.pathSeparator+macosPrefix+"../../../deployment/qtjambi-"+mod+"-"+version+".jar";
		    		if("sql".equals(mod) && !System.getProperty("qtjambi.deployer.test.jdbc", "").isEmpty()) {
		    			String jdbcPath = targetDir.toPath().toAbsolutePath().relativize(new File(System.getProperty("qtjambi.deployer.test.jdbc")).toPath().toAbsolutePath()).toString();
			    		classPath += File.pathSeparator+macosPrefix+jdbcPath;
		    		}
	    		}
	        	io.qt.qtjambi.deployer.Main.main(new String[]{
	        			"application",
	        			"--application-name="+applicationName,
	        			"--dir="+targetDir.getAbsolutePath(),
	        			"--class-path="+classPath,
	        			"--module-path="+modulePath,
	        			"--ico=C:\\",
	        			"--main-class=io.qt.autotests."+applicationName,
	        			"--jvm-path="+System.getProperty("java.home"),
	        			"--executable="+new File(deploymentDir, "QtJambiLauncher"+executable).getAbsolutePath(),
	        			"--",
	        			"-Djava.library.path="+qtBinariesPath,
	        			"-Dio.qt.debug="+(isDebug ? "debug" : "release"),
	        			"-Djava.util.logging.config.file="+System.getProperty("java.util.logging.config.file", ""),
	        			"-Xrs",
	        			"-Dio.qt.log-messages="+System.getProperty("io.qt.log-messages", "ALL"),
	        			"-Dio.qt.debug.level="+System.getProperty("io.qt.debug.level", "1"),
	        			"-Dio.qt.verbose-loading="+System.getProperty("io.qt.verbose-loading", "false")
	            	});
	    	}
	    	File testFile = new File(targetDir, applicationName+"touch.test");
	    	testFile.delete();
	    	Assert.assertTrue(!testFile.exists());
	    	UUID uuid = UUID.randomUUID();
	    	QProcessEnvironment processEnvironment = new QProcessEnvironment(QProcessEnvironment.systemEnvironment());
	    	process.setWorkingDirectory(targetDir.getAbsolutePath());
	    	process.setStandardErrorFile(new File(targetDir, applicationName+".err.log").getAbsolutePath());
	    	process.setStandardOutputFile(new File(targetDir, applicationName+".out.log").getAbsolutePath());
	    	if(osName.startsWith("windows")) {
	    		File exeFile = new File(targetDir, applicationName+".exe");
	    		Assert.assertTrue("Executable "+exeFile.getAbsolutePath()+" does not exist", exeFile.exists());
	    		process.setProgram(exeFile.getAbsolutePath());
	    		process.setArguments(new QStringList(testFile.getAbsolutePath(), uuid.toString()));
	    		processEnvironment.insert("PATH", qtBinariesPath);
	    	}else if(osName.startsWith("mac")) {
	    		File exeFile = new File(targetDir, applicationName+".app/Contents/MacOS/"+applicationName);
	    		Assert.assertTrue("Executable "+exeFile.getAbsolutePath()+" does not exist", exeFile.exists());
	    		Assert.assertTrue("Executable "+exeFile.getAbsolutePath()+" cannot be executed", exeFile.canExecute());
	    		switch(System.getProperty("os.arch").toLowerCase()) {
	        	case "arm64":
	        	case "aarch64":
	        		process.setProgram("/usr/bin/arch");
		    		process.setArguments(new QStringList("-arm64", "-e", "DYLD_FRAMEWORK_PATH=" + qtBinariesPath, exeFile.getAbsolutePath(), testFile.getAbsolutePath(), uuid.toString()));
	    			break;
	        	case "x86_64":
	        	case "x64":
	        	case "amd64":
	        		process.setProgram("/usr/bin/arch");
		    		process.setArguments(new QStringList("-x86_64", "-e", "DYLD_FRAMEWORK_PATH=" + qtBinariesPath, exeFile.getAbsolutePath(), testFile.getAbsolutePath(), uuid.toString()));
	    			break;
	    		default:
	    			process.setProgram(exeFile.getAbsolutePath());
		    		process.setArguments(new QStringList(testFile.getAbsolutePath(), uuid.toString()));
		    		processEnvironment.insert("DYLD_FRAMEWORK_PATH", qtBinariesPath);
	    		}
	    		//if(!new File(targetDir, applicationName+".app/Contents/Frameworks").exists())
	    		//	Files.createSymbolicLink(new File(targetDir, applicationName+".app/Contents/Frameworks").toPath(), new File(qtBinariesPath).toPath());
	    	}else {
	    		File exeFile = new File(targetDir, applicationName);
	    		Assert.assertTrue("Executable "+exeFile.getAbsolutePath()+" does not exist", exeFile.exists());
	    		Assert.assertTrue("Executable "+exeFile.getAbsolutePath()+" cannot be executed", exeFile.canExecute());
	    		process.setProgram(exeFile.getAbsolutePath());
	    		process.setArguments(new QStringList(testFile.getAbsolutePath(), uuid.toString()));
	    		processEnvironment.insert("LD_LIBRARY_PATH", qtBinariesPath);
	    	}
	    	process.setProcessEnvironment(processEnvironment);
	    	process.start(QIODevice.OpenModeFlag.ReadWrite);
	    	processName = ""+process.processId();
			jambiDeploymentDir = new File(tmpDir, "QtJambi" + version + "_" + System.getProperty("user.name") + "_" + processName);
			while(!process.waitForFinished(500)) {
				if(Thread.interrupted())
					throw new InterruptedException();
			}
			if(process.state()==QProcess.ProcessState.Running) {
				try{
					process.terminate();
				}catch(Throwable t) {}
			}
	    	Assert.assertTrue("Test file does not exist, i.e. standalone program did not run. Process returned: "+process.exitCode(), testFile.exists());
	    	Assert.assertEquals(uuid.toString(), new String(Files.readAllBytes(testFile.toPath())));
    	}catch(InterruptedException e) {
    	}finally {
    		if(process.state()==QProcess.ProcessState.Running) {
    			process.write("quit".getBytes());
    			process.closeWriteChannel();
    			if(process.state()==QProcess.ProcessState.Running) {
    				process.kill();
    			}
    		}
    		if((processName==null || processName.isEmpty()) && new File(targetDir, "pid").isFile()) {
    			processName = new String(Files.readAllBytes(new File(targetDir, "pid").toPath())).trim();
				jambiDeploymentDir = new File(tmpDir, "QtJambi" + version + "_" + System.getProperty("user.name") + "_" + processName);
    		}
    		process.waitForFinished();
    		process.dispose();
    		System.gc();
    		synchronized(ApplicationInitializer.class) {
    			Thread.sleep(1000);
    		}
    		System.gc();
			try {
				String content = new String(Files.readAllBytes(new File(targetDir, applicationName+".out.log").toPath()));
				if(!content.trim().isEmpty()) {
					System.out.println("##### "+applicationName+".out.log begin #####");
					System.out.println(content);
					System.out.println("###### "+applicationName+".out.log end ######");
				}
			} catch (Throwable e) {}
			try {
				String content = new String(Files.readAllBytes(new File(targetDir, applicationName+".err.log").toPath()));
				if(!content.trim().isEmpty()) {
					System.err.println("##### "+applicationName+".err.log begin #####");
					System.err.println(content);
					System.err.println("###### "+applicationName+".err.log end ######");
				}
			} catch (Throwable e) {}
			Preferences preferences = Preferences.userNodeForPackage(io.qt.internal.QtJambiInternal.class);
			String dirs = preferences.get("qtjambi.previous.deployment.dir", null);
    		if(dirs!=null && !dirs.isEmpty()) {
    	        preferences.remove("qtjambi.previous.deployment.dir");
    	        for(String dir : dirs.split("\\"+File.pathSeparator)) {
    	        	final File jambiTempDir = new File(dir);
        			if(jambiTempDir.exists() && jambiTempDir.isDirectory()) {
        				Logger.getLogger("io.qt.autotests").log(Level.FINEST, ()->"Cleaning previous deployment directory "+jambiTempDir);
        				clearAndDelete(jambiTempDir);
        			}
    	        }
    		}
    		if(jambiDeploymentDir!=null && jambiDeploymentDir.isDirectory()) {
    			Logger.getLogger("io.qt.autotests").log(Level.FINEST, "Cleaning lib directory "+jambiDeploymentDir);
    			clearAndDelete(jambiDeploymentDir);
    		}
    		if(!Boolean.getBoolean("qtjambi.deployer.skip.deletion")) {
    			Logger.getLogger("io.qt.autotests").log(Level.FINEST, ()->"Cleaning deployer directory "+targetDir);
    			clearAndDelete(targetDir);
    		}
    	}
    }
    
    static void clearAndDelete(File directory) {
    	if(directory!=null) {
    		if(directory.isDirectory()) {
		    	for(File file : directory.listFiles()) {
		    		if(file.getName().equals(".") || file.getName().equals(".."))
		    			continue;
		    		if(file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
		    			clearAndDelete(file);
		    		}else {
		    			file.delete();
		    		}
		    	}
    		}
	    	directory.delete();
    	}
    }
}