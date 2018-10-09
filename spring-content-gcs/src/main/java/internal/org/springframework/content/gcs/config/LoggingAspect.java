/**
 * 
 *//*
package internal.org.springframework.content.gcs.config;

*//**
 * @author sandip.bhoi
 *
 *//*
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Before;

@Aspect
public class LoggingAspect {
  @Pointcut("within (de.scrum_master.app..*) && execution(* *(..))")
  private void loggingTargets() {}

  @Before("loggingTargets()")
  public void logEnterMethod(JoinPoint thisJoinPoint) {
    System.out.println("ENTER " + thisJoinPoint);
  }

  @AfterReturning(pointcut = "loggingTargets()", returning = "result")
  public void logExitMethod(JoinPoint thisJoinPoint, Object result) {
    System.out.println("EXIT  " + thisJoinPoint + " -> return value = " + result);
  }

  @AfterThrowing(pointcut = "loggingTargets()", throwing = "exception")
  public void logException(JoinPoint thisJoinPoint, Exception exception) {
    System.out.println("ERROR " + thisJoinPoint + " -> exception = " + exception);
  }
}*/