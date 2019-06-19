/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.ws.test.client.RequestMatchers;
import org.springframework.ws.test.client.ResponseCreators;
import org.springframework.ws.test.support.SourceAssertionError;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebServiceClientTest @WebServiceClientTest}.
 *
 * @author Dmytro Nosan
 */
@WebServiceClientTest(ExampleWebServiceClient.class)
class WebServiceClientIntegrationTests {

	@Autowired
	private MockWebServiceServer mockWebServiceServer;

	@Autowired
	private ExampleWebServiceClient client;

	@Test
	void mockServerCall() {
		this.mockWebServiceServer.expect(RequestMatchers.payload(new StringSource("<request/>"))).andRespond(
				ResponseCreators.withPayload(new StringSource("<response><status>200</status></response>")));
		assertThat(this.client.test()).extracting(Response::getStatus).isEqualTo(200);
	}

	@Test
	void mockServerCall1() {
		this.mockWebServiceServer.expect(RequestMatchers.connectionTo("https://example1"))
				.andRespond(ResponseCreators.withPayload(new StringSource("<response/>")));
		assertThatExceptionOfType(SourceAssertionError.class).isThrownBy(this.client::test)
				.withMessageContaining("Unexpected connection expected");
	}

	@Test
	void mockServerCall2() {
		this.mockWebServiceServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(ResponseCreators.withError("Invalid Request"));
		assertThatExceptionOfType(WebServiceTransportException.class).isThrownBy(this.client::test)
				.withMessageContaining("Invalid Request");
	}

}
