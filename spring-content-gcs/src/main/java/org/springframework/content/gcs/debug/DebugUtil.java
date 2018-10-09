package org.springframework.content.gcs.debug;

public class DebugUtil {
	private static int counter = 0;

	public synchronized static void printCurrentMethod(String msg) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		System.out.println("[spring-content-gcs]: " + counter++ + " from " + stackTrace[2].getClassName() + " "
				+ stackTrace[2].getLineNumber() + " " + stackTrace[2].getMethodName() + " " + msg);
	}

	public synchronized static void printCurrentMethod() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		System.out.println("[spring-content-gcs]: " + counter++ + " from " + stackTrace[2].getClassName() + " "
				+ stackTrace[2].getLineNumber() + " " + stackTrace[2].getMethodName());
	}
}
