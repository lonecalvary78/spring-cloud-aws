/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.autoconfigure;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

/**
 * Base callback interface to be extended by customizers for specific AWS clients.
 *
 * @param <T> - type of AWS client builder to customize
 * @author Maciej Walkowiak
 * @since 3.3.0
 */
public interface AwsClientCustomizer<T extends AwsClientBuilder<?, ?>> {

	/**
	 * Callback to customize an instance of AWS client builder.
	 *
	 * @param builder the client builder to customize
	 */
	void customize(T builder);
}
