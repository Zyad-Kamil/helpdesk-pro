package com.helpdeskpro.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.helpdeskpro.service.AbstractServiceTests.TestClockConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service tests run without a surrounding transaction: each service call has its own, as in production,
 * so any lazy loading left in a returned object would fail.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Import({ TicketService.class, TechnicianService.class, TechnicianProfileService.class, CategoryService.class,
		TagService.class, TestClockConfig.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@interface ServiceTest {

}
