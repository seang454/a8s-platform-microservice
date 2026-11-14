package co.istad.a8scommonsecurity.annotation;

import co.istad.a8scommonsecurity.config.A8sResourceServerSecurityConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable A8S OAuth2 Resource Server security on any microservice.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(A8sResourceServerSecurityConfig.class)
public @interface EnableA8sResourceServer {
}
