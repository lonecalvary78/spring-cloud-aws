/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.sqs.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.config.Endpoint;
import io.awspring.cloud.sqs.config.MultiMethodSqsEndpoint;
import io.awspring.cloud.sqs.config.SqsBeanNames;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.support.resolver.BatchVisibilityHandlerMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.NotificationMessageArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.NotificationSubjectArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.QueueAttributesMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.SnsNotificationArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.SqsMessageMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.VisibilityHandlerMethodArgumentResolver;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.ReflectionUtils;

/**
 * {@link AbstractListenerAnnotationBeanPostProcessor} implementation for {@link SqsListener @SqsListener}.
 *
 * @author Tomaz Fernandes
 * @author Joao Calassio
 * @author José Iêdo
 * @since 3.0
 */
public class SqsListenerAnnotationBeanPostProcessor extends AbstractListenerAnnotationBeanPostProcessor<SqsListener> {

	private static final String GENERATED_ID_PREFIX = "io.awspring.cloud.sqs.sqsListenerEndpointContainer#";

	@Override
	protected Class<SqsListener> getAnnotationClass() {
		return SqsListener.class;
	}

	protected Endpoint createEndpoint(SqsListener sqsListenerAnnotation) {
		return SqsEndpoint.builder().queueNames(resolveEndpointNames(sqsListenerAnnotation.value()))
				.factoryBeanName(resolveAsString(sqsListenerAnnotation.factory(), "factory"))
				.id(getEndpointId(sqsListenerAnnotation.id()))
				.pollTimeoutSeconds(resolveAsInteger(sqsListenerAnnotation.pollTimeoutSeconds(), "pollTimeoutSeconds"))
				.maxMessagesPerPoll(resolveAsInteger(sqsListenerAnnotation.maxMessagesPerPoll(), "maxMessagesPerPoll"))
				.maxConcurrentMessages(
						resolveAsInteger(sqsListenerAnnotation.maxConcurrentMessages(), "maxConcurrentMessages"))
				.messageVisibility(
						resolveAsInteger(sqsListenerAnnotation.messageVisibilitySeconds(), "messageVisibility"))
				.acknowledgementMode(resolveAcknowledgement(sqsListenerAnnotation.acknowledgementMode())).build();
	}

	@Override
	protected Endpoint createMultiMethodEndpoint(SqsListener sqsListenerAnnotation, List<Method> methods,
			@Nullable Method defaultMethod, Object bean) {
		return MultiMethodSqsEndpoint.builder()
				.factoryBeanName(resolveAsString(sqsListenerAnnotation.factory(), "factory"))
				.queueNames(resolveEndpointNames(sqsListenerAnnotation.value())).bean(bean).methods(methods)
				.defaultMethod(defaultMethod).id(getEndpointId(sqsListenerAnnotation.id()))
				.sqsEndpoint(createEndpoint(sqsListenerAnnotation)).build();
	}

	@Override
	protected Set<Method> getHandlerMethods(Class<?> targetClass) {
		return MethodIntrospector.selectMethods(targetClass, (ReflectionUtils.MethodFilter) method -> AnnotationUtils
				.findAnnotation(method, SqsHandler.class) != null);
	}

	@Override
	protected String getGeneratedIdPrefix() {
		return GENERATED_ID_PREFIX;
	}

	@Override
	protected String getMessageListenerContainerRegistryBeanName() {
		return SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME;
	}

	@Override
	protected Collection<HandlerMethodArgumentResolver> createAdditionalArgumentResolvers() {
		return Arrays.asList(new VisibilityHandlerMethodArgumentResolver(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER),
				new BatchVisibilityHandlerMethodArgumentResolver(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER),
				new SqsMessageMethodArgumentResolver(), new QueueAttributesMethodArgumentResolver());
	}

	@Override
	protected Collection<HandlerMethodArgumentResolver> createAdditionalArgumentResolvers(
			MessageConverter messageConverter, ObjectMapper objectMapper) {
		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>(createAdditionalArgumentResolvers());
		if (objectMapper != null) {
			argumentResolvers.add(new NotificationMessageArgumentResolver(messageConverter, objectMapper));
			argumentResolvers.add(new NotificationSubjectArgumentResolver(objectMapper));
			argumentResolvers.add(new SnsNotificationArgumentResolver(messageConverter, objectMapper));
		}
		return argumentResolvers;
	}

	@Override
	protected Method getDefaultHandlerMethod(Class<?> targetClass, List<Method> handlerMethods) {
		Method defaultMethod = null;
		for (Method method : handlerMethods) {
			SqsHandler annotation = method.getAnnotation(SqsHandler.class);
			if (annotation.isDefault()) {
				if (defaultMethod != null) {
					throw new IllegalArgumentException(
							"There is more than one default method for the same listener in class: " + targetClass);
				}
				else {
					defaultMethod = method;
				}
			}
		}
		return defaultMethod;
	}
}
