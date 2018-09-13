/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client;

import java.time.Duration;

// TODO: Issue #1133 - https://github.com/zeebe-io/zeebe/issues/1133
public interface ZeebeClientConfiguration {
  /** @see ZeebeClientBuilder#brokerContactPoint(String) */
  String getBrokerContactPoint();

  /** @see ZeebeClientBuilder#requestTimeout(Duration) */
  Duration getRequestTimeout();

  /** @see ZeebeClientBuilder#requestBlocktime(Duration) */
  Duration getRequestBlocktime();

  /** @see ZeebeClientBuilder#sendBufferSize(int) */
  int getSendBufferSize();

  /** @see ZeebeClientBuilder#numManagementThreads(int) */
  int getNumManagementThreads();

  /** @see ZeebeClientBuilder#numSubscriptionExecutionThreads(int) */
  int getNumSubscriptionExecutionThreads();

  /** @see ZeebeClientBuilder#defaultTopicSubscriptionBufferSize(int) */
  int getDefaultTopicSubscriptionBufferSize();

  /** @see ZeebeClientBuilder#defaultJobSubscriptionBufferSize(int) */
  int getDefaultJobSubscriptionBufferSize();

  /** @see ZeebeClientBuilder#tcpChannelKeepAlivePeriod(Duration) */
  Duration getTcpChannelKeepAlivePeriod();

  /** @see ZeebeClientBuilder#defaultJobWorkerName(String) */
  String getDefaultJobWorkerName();

  /** @see ZeebeClientBuilder#defaultJobTimeout(Duration) */
  Duration getDefaultJobTimeout();

  /** @see ZeebeClientBuilder#defaultMessageTimeToLive(Duration) */
  Duration getDefaultMessageTimeToLive();
}
